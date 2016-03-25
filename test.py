#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import absolute_import, print_function
import numpy as np
import pyopencl as cl
import time
from matplotlib import pyplot as plt
from PIL import Image
import sys


ctx = cl.create_some_context()
queue = cl.CommandQueue(ctx)
script_filename = sys.argv[0]
cl_filename = script_filename.split('.')[0] + '.cl'

with open(cl_filename, 'r') as cl_file:
    prg = cl.Program(ctx, cl_file.read()).build()

mf = cl.mem_flags
WIDTH = 848
HEIGHT = 640
#print(conv)
#in_np = np.zeros((WIDTH, HEIGHT), np.float32)
in_np = np.array(Image.open(sys.argv[1]))
in_np = np.array(in_np, np.float32, copy=True)/255.0
print(in_np)
print(in_np.shape)
plt.imshow(in_np, interpolation='none', cmap='gray')
plt.show()


conv = np.full((in_np.shape[0], in_np.shape[1], 3,3), 1.0/9.0, np.float32)
conv_g = cl.Buffer(ctx, mf.READ_WRITE | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=conv)

in_g = cl.Buffer(ctx, mf.READ_WRITE | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=in_np)
res_np = np.zeros(in_np.shape, np.float32)
res_g = cl.Buffer(ctx, mf.READ_WRITE | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=res_np)

start_time = time.time()
#prg.enumerate(queue, res_np.shape, None, res_g, np.int32(res_np.shape[0]), np.int32(res_np.shape[1]))
for x in range(1000):
    #prg.hconvolve(queue, res_np.shape, None, in_g, res_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]), conv_g, np.int32(3), np.int32(3))
    #prg.hconvolve(queue, res_np.shape, None, res_g, in_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]), conv_g, np.int32(3), np.int32(3))
    #cl.enqueue_copy(queue, in_g, res_g)
    prg.gauss(queue, res_np.shape, (16,16), in_g, res_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]),np.int32(9), np.int32(9))
    prg.gauss(queue, res_np.shape, (16,16), res_g, in_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]),np.int32(9), np.int32(9))

prg.gauss(queue, res_np.shape, (16,16), in_g, res_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]),np.int32(9), np.int32(9))

cl.enqueue_copy(queue, res_np, res_g)
print('Done mafaka')
print(time.time() - start_time)
plt.imshow(res_np, interpolation='none', cmap='gray')
plt.show()
#print(res_np)
#print(res_np[2][2])
print(res_np[150][150])

