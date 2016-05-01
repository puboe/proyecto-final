#!/usr/bin/env python
# -*- coding: utf-8 -*-

import numpy as np
import pyopencl as cl
import time
from matplotlib import pyplot as plt
from PIL import Image
import sys
import os
import shutil
import pickle


ctx = cl.create_some_context()
queue = cl.CommandQueue(ctx)
script_filename = sys.argv[0]
cl_filename = script_filename.split('.')[0] + '.cl'

with open(cl_filename, 'r') as cl_file:
    prg = cl.Program(ctx, cl_file.read()).build()

mf = cl.mem_flags
WIDTH = 848
HEIGHT = 640
image_filenames = sys.argv[1:]
#image_filenames.sort()
print(image_filenames)

os_np = [np.array(Image.open(f), np.float32, copy=True, order='F')/255.0
        for f in image_filenames]

ishape = os_np[0].shape

conv_dim = 25
win_dim = 23
#conv_dim = 9
#win_dim = 11

os_g = [cl.Buffer(ctx, mf.READ_WRITE | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=s) for s in os_np]


#s_np = [np.zeros_like(s) for s in os_np]
#s_g = [cl.Buffer(ctx, mf.READ_WRITE | mf.ALLOC_HOST_PTR, size=s.size) for s in os_g]

#for idx in range(len(os_g)):
    #prg.gradient(queue, ishape, None, os_g[idx], s_g[idx]).wait()
    ##cl.enqueue_copy(queue, s_g[idx], os_g[idx]).wait()
    #cl.enqueue_copy(queue, s_np[idx], s_g[idx]).wait()

s_np = os_np
s_g = os_g


#gs_g = [cl.Buffer(ctx, mf.READ_WRITE | mf.ALLOC_HOST_PTR, size=s.size) for s in s_g]

dx = np.zeros_like(s_np[0], order='F')
dy = np.zeros_like(s_np[0], order='F')

ds_x = 10
ds_y = 10
dx_ds = np.zeros((dx.shape[0]/ds_x, dx.shape[1]/ds_y), np.float32, order='F')
dy_ds = np.zeros((dy.shape[0]/ds_x, dy.shape[1]/ds_y), np.float32, order='F')

dx_g = cl.Buffer(ctx, mf.WRITE_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=dx)
dy_g = cl.Buffer(ctx, mf.WRITE_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=dy)

dx_ds_g = cl.Buffer(ctx, mf.WRITE_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=dx_ds)
dy_ds_g = cl.Buffer(ctx, mf.WRITE_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=dy_ds)
start_time = time.time()


prg.best_delta(queue, ishape, None, s_g[0], s_g[1],
               dx_g, dy_g,
               np.int32(conv_dim), np.int32(conv_dim),
               np.int32(win_dim), np.int32(win_dim)).wait()


prg.downsample2d(queue, dx_ds.shape, None, dx_ds_g, dx_g,
                 np.int32(ds_x), np.int32(ds_y))

prg.downsample2d(queue, dy_ds.shape, None, dy_ds_g, dy_g,
                 np.int32(ds_x), np.int32(ds_y))


#prg.bma(queue, dx_ds.shape, (10, 10), s_g[0], s_g[1],
        #dx_ds_g, dy_ds_g,
        #np.int32(conv_dim), np.int32(conv_dim),
        #np.int32(ds_x), np.int32(ds_y))

cl.enqueue_copy(queue, dx_ds, dx_ds_g)
cl.enqueue_copy(queue, dy_ds, dy_ds_g)
#cl.enqueue_copy(queue, dx, dx_g)
#cl.enqueue_copy(queue, dy, dy_g)


for (i, o) in zip(s_np, s_g):
    cl.enqueue_copy(queue, i, o)

print('Done mafaka')
print(dx_ds.shape, dy_ds.shape)
print(dx.shape, dy.shape)
print(time.time() - start_time)

#plt.imshow(s_np[0], interpolation='none', cmap='gray')
#Y, X = np.mgrid[0:ishape[0]:1, 0:ishape[1]:1]
#plt.quiver(X, Y, dx, dy, units='xy', color='red')
#plt.show()

print(np.max(dx), np.max(dy))
print(np.max(dx_ds), np.max(dy_ds))

print(np.min(dx),    np.min(dy))
print(np.min(dx_ds), np.min(dy_ds))

plt.imshow(s_np[0]/2 + s_np[1]/2, interpolation='none', cmap='gray')
Y, X = np.mgrid[0:ishape[0]:ds_x, 0:ishape[1]:ds_y]
plt.quiver(X, Y, dy_ds, -dx_ds, scale=1.0, units='xy', color='red')
plt.show()



if __name__ == "__main__":
    main()
