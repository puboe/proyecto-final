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
from collections import namedtuple
from svg import SVGImage

import logging
import loggingtools
logging.basicConfig(stream=sys.stderr, level=logging.DEBUG)
logger = logging.getLogger("Meteo")
timefunction = loggingtools.create_timefunction(logger)
logblock = loggingtools.create_logblock(logger)

mf = cl.mem_flags

def arange2d(starts, stops, steps, dtype=None):
    x, y = [np.arange(start, stop, step)
            for start, stop, step in zip(starts, stops, steps)]
    return np.transpose([np.tile(x, len(y)), np.repeat(y, len(x))])

def crop_image(image, crop_rect = None):
    if crop_rect is None:
        return image
    (x, y, w, h) = crop_rect
    return np.array(image[y:y+h, x:x+w], np.float32, copy=False, order='F')


class MeteoState(object):
    @classmethod
    def from_image(cls, image_file, clargs, crop_rect=None):
        data = np.array(Image.open(image_file), np.float32, copy=True, order='F')/255.0

        return cls(datetime.strptime(os.path.basename(image_file)[:10], "%y%m%d%H%M"),
                   crop_image(data, crop_rect), clargs, inject=True)

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
        return np.any(self.ir_data > 0)

    def get_datetime(self):
        return self.datetime 

class MeteoStep(object):
    StatePair = namedtuple('StatePair', ['prev', 'post'])
    def __init__(self, states, search_area, window, downsample, clargs):
        states = MeteoStep.StatePair(*states)
        if states.prev.get_shape() != states.post.get_shape():
            raise Exception('Step states must be of the same shape')

        self.states = states
        self.search_area = search_area
        self.window = window
        self.downsample = downsample
        self.clargs = clargs

        self.dx = None
        self.dy = None
        self.dx_ds = None
        self.dy_ds = None

        #self.density_data_key = 'get_ir_data_edges'
        self.density_data_key = 'get_ir_data'

    def _gen_motion_data(self):
        prev = getattr(self.states.prev, self.density_data_key)()
        post = getattr(self.states.post, self.density_data_key)()

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

    def trail_step(self, pos, backwards = False):
        dx_ds, dy_ds = self.get_ds_motion_data()
        pos_index = tuple(np.transpose(np.fliplr(pos.astype(np.uint32)) // self.downsample))
        dpos = np.transpose(np.array([dy_ds[pos_index], dx_ds[pos_index]]))
        return pos - dpos if backwards else pos + dpos

    def get_timedelta(self):
        return self.states.post.get_datetime() - self.states.prev.get_datetime()

    def get_shape(self):
        return self.states.prev.get_shape()



class MeteoFlux(object):
    @classmethod
    def from_images(cls, search_area, window, downsample, image_files, clargs, crop_rect=None):
        states = [MeteoState.from_image(image_file, clargs, crop_rect)
                    for image_file in image_files]
        return cls.from_states(states, search_area, window, downsample, clargs)

    @classmethod
    def from_states(cls, states, search_area, window, downsample, clargs):
        states = list(sorted(list(filter(lambda s: s.valid(), states)),
                             key=lambda s: s.get_datetime()))

        if len(states) < 2:
            raise Exception('Need at least two states to calculate steps')

        steps = [MeteoStep(pair, search_area, window, downsample, clargs)
                   for pair in zip(states[:-1], states[1:])]

        return cls(steps, clargs)

    def __init__(self, steps, clargs):
        self.steps = steps
        self.clargs = clargs

    def get_states(self):
        yield self.steps[0].states.prev
        for step in self.steps:
            yield step.states.post

    def get_shape(self):
        return self.steps[0].get_shape()

    def get_trail(self, start, transpose = False, backwards = False):
        if not backwards:
            result = np.array(reduce(lambda slist, step: slist + [step.trail_step(slist[-1])],
                                     self.steps, [start]))
        else:
            result = np.array(reduce(lambda slist, step: [step.trail_step(slist[0], backwards=True)] + slist,
                                     self.steps[::-1], [start]))
        return np.transpose(result, (1, 0, 2)) if transpose else result

    def get_times(self):
        return np.cumsum([0.0] + [step.get_timedelta().seconds // 60
                            for step in self.steps])

    def get_polys(self, start, deg, backwards = False):
        trails = self.get_trail(start, transpose = True, backwards = backwards)
        t = self.get_times()
        return [(np.polyfit(t, c, deg) for c in np.transpose(trail))
                for trail in trails]

    def get_polyfitted_trails(self, tstart, tstop, tstep, start, deg, backwards = False):
        polys = self.get_polys(start, deg, backwards = backwards)
        return [(np.polyval(poly, np.arange(tstart, tstop, tstep)) for poly in polypair)
                    for polypair in polys]


class MeteoMap(object):
    @classmethod
    def from_image(cls, image_file, crop_rect = None):
        image = np.array(Image.open(image_file), np.float32, copy=True, order='F')/255.0
        return cls(crop_image(image, crop_rect))

    @classmethod
    def to_image(cls, mmap, image_file):
        image = Image.fromarray((mmap.image*255.0).astype(np.uint8))
        image.save(image_file)

    def __init__(self, image):
        self.image = image


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
    crop_rect = crop_rect=(180, 160, 500, 300)  
    flux = MeteoFlux.from_images((15, 15), (33, 33), (ds_x, ds_y), images, clargs, crop_rect = None)
    mmap = MeteoMap.from_image("map.png", crop_rect = crop_rect)

    print(flux.get_times())

    start = np.fliplr(arange2d((10.0, 10.0), flux.get_shape(), (10.0, 10.0), dtype=np.float32) - 10.0)
    trail = flux.get_trail(start, backwards = True)

    for index, (state, points) in enumerate(zip(flux.get_states(), trail)):
        x, y = np.transpose(points)
        height, width = flux.get_shape()
        plt.xlim([0, width])
        plt.ylim([0, height])
        plt.gca().invert_yaxis()
        plt.imshow(state.get_ir_data(), interpolation='none', cmap='gray')
        plt.plot(x, y, 'ro')
        plt.savefig('result/' + str(index) + '.png', format='png')
        plt.clf()

    plt.imshow(mmap.image, interpolation='none', cmap='gray')
    for (v, dv) in zip(trail[:-1], np.diff(trail, axis=0)):
        x, y = np.transpose(v)
        dx, dy = np.transpose(dv)
        plt.quiver(x, y, dx, -dy, scale=1.0, units='x', color='blue')

    plt.show()

    start_time = 0.0
    end_time = np.max(flux.get_times()) + 60.0

    MeteoMap.to_image(mmap, "result/map.png")
    svg = SVGImage("map.png", "result/lines.svg", flux.get_shape())

    plt.imshow(mmap.image, interpolation='none', cmap='gray')
    for (tx, ty) in flux.get_polyfitted_trails(start_time, end_time, 5.0, start, 2):
        plt.plot(tx, ty, 'b')
        txy = list(zip(tx, ty))
        for v1, v2 in zip(txy[:-1], txy[1:]):
            svg.addLine(v1, v2)
    svg.save()


    plt.show()

if __name__ == "__main__":
    main()

