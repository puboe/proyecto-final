import numpy as np

import os
import sys
import logging
from . import loggingtools
logging.basicConfig(stream=sys.stderr, level=logging.DEBUG)
logger = logging.getLogger("image")
timefunction = loggingtools.create_timefunction(logger)
logblock = loggingtools.create_logblock(logger)


class ImageProcessor(object):
    def gradient(self, image):
        raise NotImplementedError()

    def bma(self, prev, post, search_area, window):
        raise NotImplementedError()

    def convolve(self, image, convmat):
        raise NotImplementedError()

    def gauss(self, image, size):
        self.convolve(image, np.ones((size, size))/(size*size))

    def downsample(self, image, ds):
        raise NotImplementedError()

    def crop(self, image, crop_rect = None):
        if crop_rect is None:
            return image
        (x, y, w, h) = crop_rect
        return np.array(image[y:y+h, x:x+w], np.float32, copy=False, order='F')

    def is_black(self, image):
        return np.all(image == 0)


class CLImageProcessor(ImageProcessor):
    def __init__(self, pyopencl, context=None, queue=None, program=None):
        if context is None:
            if queue is not None:
                raise ValueError('Cannot pass queue without context.')
            if program is not None:
                raise ValueError('Cannot pass program without context.')

        self.pyopencl = pyopencl
        self.context = context or self.pyopencl.create_some_context()
        self.queue = queue or self.pyopencl.CommandQueue(self.context)

        self.program = program
        if self.program is None:
            script_file_name = 'image.cl'
            script_file_path = os.path.join(os.path.dirname(__file__), script_file_name)
            with open(script_file_path, 'r') as cl_file:
                self.program = self.pyopencl.Program(self.context, cl_file.read()).build()

    def _check_fortran_cont(self, **kwargs):
        for k, v in kwargs.items():
            if not v.flags['F_CONTIGUOUS']:
                raise ValueError("Parameter '%s' must be Fortran-contiguous" % (k,))

    #@timefunction
    def gradient(self, image):
        cl = self.pyopencl
        mf = cl.mem_flags

        self._check_fortran_cont(image=image)

        gradient = np.empty_like(image, order='F')

        image_g = cl.Buffer(self.context,
                              mf.READ_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR,
                              hostbuf=image)
        gradient_g = cl.Buffer(self.context,
                                 mf.WRITE_ONLY | mf.ALLOC_HOST_PTR,
                                 size=gradient.nbytes)

        with logblock("gradient-kernel"):
            self.program.gradient(self.queue, gradient.shape, None,
                                            image_g, gradient_g).wait()

        cl.enqueue_copy(self.queue, gradient, gradient_g).wait()
                                
        return gradient

    #@timefunction
    def bma(self, prev, post, search_area, window):
        cl = self.pyopencl
        mf = cl.mem_flags

        self._check_fortran_cont(prev=prev, post=post)

        print('shape')
        print(prev.shape)

        dx = np.empty_like(prev, order='F')
        dy = np.empty_like(prev, order='F')

        prev_g = cl.Buffer(self.context,
                              mf.READ_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR,
                              hostbuf=prev)
        post_g = cl.Buffer(self.context,
                              mf.READ_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR,
                              hostbuf=post)

        dx_g = cl.Buffer(self.context,
                             mf.READ_WRITE | mf.ALLOC_HOST_PTR,
                             size=prev.nbytes)
        dy_g = cl.Buffer(self.context,
                             mf.READ_WRITE | mf.ALLOC_HOST_PTR,
                             size=prev.nbytes)

        window_w, window_h = window
        search_area_w, search_area_h = search_area

        with logblock("bma-kernel"):
            self.program.best_delta(self.queue, prev.shape, None,
                                          prev_g, post_g,
                                          dx_g, dy_g,
                                          np.int32(search_area_w), np.int32(search_area_h),
                                          np.int32(window_w), np.int32(window_h)).wait()

        cl.enqueue_copy(self.queue, dx, dx_g).wait()
        cl.enqueue_copy(self.queue, dy, dy_g).wait()

        return dx, dy


    @timefunction
    def downsample(self, image, ds):
        cl = self.pyopencl
        mf = cl.mem_flags

        self._check_fortran_cont(image=image)

        ds_x, ds_y = ds
        (h, w) = image.shape

        image_ds = np.empty((h//ds_y, w//ds_x), np.float32, order='F')

        image_ds_g = cl.Buffer(self.context,
                            mf.WRITE_ONLY | mf.ALLOC_HOST_PTR,
                            size=image_ds.nbytes)

        image_g = cl.Buffer(self.context,
                              mf.READ_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR,
                              hostbuf=image)

        self.program.downsample2d(self.queue, image_ds.shape, None, image_ds_g, image_g,
                                    np.int32(ds_x), np.int32(ds_y))

        cl.enqueue_copy(self.queue, image_ds, image_ds_g).wait()

        return image_ds



if __name__ == '__main__':
    #import code
    import pyopencl as cl
    p = CLImageProcessor(cl)
    ident = np.asfortranarray(np.identity(9, dtype=np.float32))
    print(ident)
    print(p.gradient(ident))
    print(p.bma(ident, ident, (3, 3), (3, 3)))
    print(p.downsample(np.asfortranarray(np.ones((9, 9), dtype=np.float32)) , (3, 3)))

    #code.interact(local=locals())
