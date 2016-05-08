import numpy as np
import pyopencl as cl
import time
from PIL import Image
import sys
import os
import shutil
import pickle
from functools import reduce
from datetime import datetime

import logging
import loggingtools
logging.basicConfig(stream=sys.stderr, level=logging.DEBUG)
logger = logging.getLogger("Meteo")
timefunction = loggingtools.create_timefunction(logger)
logblock = loggingtools.create_logblock(logger)

mf = cl.mem_flags


class MeteoState(object):
    @classmethod
    def from_image(cls, image_file, clargs, crop_rect=None):
        data = np.array(Image.open(image_file), np.float32, copy=True, order='F')/255.0

        if crop_rect is not None:
            (x, y, w, h) = crop_rect
            data = np.array(data[y:y+h, x:x+w], np.float32, copy=False, order='F')

        return cls(datetime.strptime(os.path.basename(image_file)[:10], "%y%m%d%H%M"),
                   data, clargs, inject=True)

    def __init__(self, datetime, ir_data, clargs, inject=False):
        if len(ir_data.shape) != 2:
            raise Exception('Only bidimensional data allowed')
        self.clargs = clargs
        self.ir_data = ir_data if inject else np.array(ir_data, np.float32, copy=True, order='F')
        self.ir_data_edges = None
        self.datetime = datetime

    def _gen_ir_data_edges(self):
        output = np.zeros_like(self.ir_data, order='F')

        ir_data_g = cl.Buffer(self.clargs['ctx'],
                              mf.READ_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR,
                              hostbuf=self.ir_data)
        output_g = cl.Buffer(self.clargs['ctx'],
                             mf.WRITE_ONLY | mf.ALLOC_HOST_PTR,
                             hostbuf=output)

        self.clargs['prg'].gradient(self.clargs['q'], self.get_shape(), None,
                                ir_data_g, output_g).wait()

        cl.enqueue_copy(self.clargs['q'], output, output_g).wait()
                                
        return output


    def get_ir_data(self):
        return self.ir_data

    def get_ir_data_edges(self):
        if self.ir_data_edges is None:
            self.ir_data_edges = self._gen_ir_data_edges()
        return self.ir_data_edges

    def get_shape(self):
        return self.ir_data.shape

    def valid(self):
        print(np.any(self.ir_data > 0))
        return np.any(self.ir_data > 0)

    def get_datetime(self):
        return self.datetime 

class MeteoStep(object):
    def __init__(self, states, search_area, window, downsample, clargs):
        prev_state, post_state = states
        if prev_state.get_shape() != post_state.get_shape():
            raise Exception('Step states must be of the same shape')

        self.prev_state = prev_state
        self.post_state = post_state
        self.search_area = search_area
        self.window = window
        self.downsample = downsample
        self.clargs = clargs

        self.dx = None
        self.dy = None
        self.dx_ds = None
        self.dy_ds = None

        self.density_data_key = 'get_ir_data_edges'

    def _gen_motion_data(self):
        prev = getattr(self.prev_state, self.density_data_key)()
        post = getattr(self.post_state, self.density_data_key)()

        dx = np.zeros_like(prev, order='F')
        dy = np.zeros_like(dx, order='F')

        prev_g = cl.Buffer(self.clargs['ctx'],
                              mf.READ_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR,
                              hostbuf=prev)

        post_g = cl.Buffer(self.clargs['ctx'],
                              mf.READ_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR,
                              hostbuf=post)

        dx_g = cl.Buffer(self.clargs['ctx'],
                             mf.READ_WRITE | mf.ALLOC_HOST_PTR,
                             size=prev.nbytes)

        dy_g = cl.Buffer(self.clargs['ctx'],
                             mf.READ_WRITE | mf.ALLOC_HOST_PTR,
                             size=prev.nbytes)

        window_w, window_h = self.window
        search_area_w, search_area_h = self.search_area


        with logblock("bma"):
            self.clargs['prg'].best_delta(self.clargs['q'], prev.shape, None,
                                          prev_g, post_g,
                                          dx_g, dy_g,
                                          np.int32(search_area_w), np.int32(search_area_h),
                                          np.int32(window_w), np.int32(window_h)).wait()

        cl.enqueue_copy(self.clargs['q'], dx, dx_g).wait()
        cl.enqueue_copy(self.clargs['q'], dy, dy_g).wait()

        return dx, dy

    def _gen_ds_motion_data(self):
        dx, dy = self.get_motion_data()

        ds_x, ds_y = self.downsample
        (h, w) = dx.shape

        dx_ds = np.zeros((h//ds_y, w//ds_x), np.float32, order='F')
        dy_ds = np.zeros((h//ds_y, w//ds_x), np.float32, order='F')

        dx_ds_g = cl.Buffer(self.clargs['ctx'],
                            mf.READ_WRITE | mf.ALLOC_HOST_PTR,
                            size=dx_ds.nbytes)

        dy_ds_g = cl.Buffer(self.clargs['ctx'],
                            mf.READ_WRITE | mf.ALLOC_HOST_PTR,
                            size=dy_ds.nbytes)

        dx_g = cl.Buffer(self.clargs['ctx'],
                              mf.READ_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR,
                              hostbuf=dx)

        dy_g = cl.Buffer(self.clargs['ctx'],
                              mf.READ_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR,
                              hostbuf=dy)

        self.clargs['prg'].downsample2d(self.clargs['q'], dx_ds.shape, None, dx_ds_g, dx_g,
                                        np.int32(ds_x), np.int32(ds_y))

        self.clargs['prg'].downsample2d(self.clargs['q'], dy_ds.shape, None, dy_ds_g, dy_g,
                                        np.int32(ds_x), np.int32(ds_y))

        cl.enqueue_copy(self.clargs['q'], dx_ds, dx_ds_g).wait()
        cl.enqueue_copy(self.clargs['q'], dy_ds, dy_ds_g).wait()

        return dx_ds, dy_ds

    def get_motion_data(self):
        if self.dx is None or self.dy is None:
            self.dx, self.dy = self._gen_motion_data()
        return self.dx, self.dy

    def get_ds_motion_data(self):
        if self.dx_ds is None or self.dy_ds is None:
            self.dx_ds, self.dy_ds = self._gen_ds_motion_data()
        return self.dx_ds, self.dy_ds

    def trail_step(self, pos):
        dx_ds, dy_ds = self.get_ds_motion_data()
        pos_index = tuple(np.transpose(np.fliplr(pos.astype(np.uint32)) // self.downsample))
        dpos = np.transpose(np.array([dy_ds[pos_index], dx_ds[pos_index]]))
        return pos + dpos


class MeteoFlux(object):
    @classmethod
    def from_images(cls, search_area, window, downsample, image_files, clargs, crop_rect=None):
        states = [MeteoState.from_image(image_file, clargs, crop_rect)
                    for image_file in image_files]
        return cls(states, search_area, window, downsample, clargs)

    def __init__(self, states, search_area, window, downsample, clargs):
        self.states = list(sorted(list(filter(lambda s: s.valid(), states)),
                                  key=lambda s: s.get_datetime()))

        if len(self.states) < 2:
            raise Exception('Need at least two states to calculate steps')

        self.steps = [MeteoStep(pair, search_area, window, downsample, clargs)
                        for pair in zip(self.states[:-1], self.states[1:])]

    def get_trail(self, start, transpose = False):
        result = np.array(reduce(lambda slist, step: slist + [step.trail_step(slist[-1])],
                                 self.steps, [start]))
        return np.transpose(result, (1, 0, 2)) if transpose else result


def main():
    import sys
    from matplotlib import pyplot as plt
    images = sys.argv[1:]
    script_filename = sys.argv[0]
    cl_filename = script_filename.split('.')[0] + '.cl'
    cl_context = cl.create_some_context()
    cl_queue = cl.CommandQueue(cl_context)
    with open(cl_filename, 'r') as cl_file:
        cl_program = cl.Program(cl_context, cl_file.read()).build()

    clargs = {'ctx': cl_context,
              'q': cl_queue,
              'prg': cl_program}

    (ds_x, ds_y) = (10, 10)
    images.sort()
    crop_rect = None
    #crop_rect = crop_rect=(180, 160, 500, 300)  
    flux = MeteoFlux.from_images((15, 15), (33, 33), (ds_x, ds_y), images, clargs, crop_rect = crop_rect)

    trail = flux.get_trail((0.0, 0.0) + np.random.random((400, 2))*(500.0, 300.0) )

    for index, (state, points) in enumerate(zip(flux.states, trail)):
        x, y = np.transpose(points)
        plt.xlim([0, state.get_ir_data().shape[1]])
        plt.ylim([0, state.get_ir_data().shape[0]])
        plt.gca().invert_yaxis()
        plt.imshow(state.get_ir_data(), interpolation='none', cmap='gray')
        plt.plot(x, y, 'ro')
        plt.savefig('result/' + str(index) + '.png', format='png')
        plt.clf()

    plt.imshow(flux.states[0].get_ir_data(), interpolation='none', cmap='gray')

    for (v, dv) in zip(trail[:-1], np.diff(trail, axis=0)):
        x, y = np.transpose(v)
        dx, dy = np.transpose(dv)
        plt.quiver(x, y, dx, -dy, scale=1.0, units='x', color='blue')

    plt.show()

    #for index, step in enumerate(flux.steps):

        #ir_data = step.prev_state.get_ir_data()
        #bshape = ir_data.shape

        #dx_ds, dy_ds = step.get_ds_motion_data()

        #Y, X = np.mgrid[0:bshape[0]:ds_x, 0:bshape[1]:ds_y]
        #plt.figure(str(index))
        #plt.imshow(ir_data, interpolation='none', cmap='gray')
        #plt.quiver(X, Y, dy_ds, -dx_ds, scale=1.0, units='xy', color='red')

    #plt.show()

if __name__ == "__main__":
    main()

