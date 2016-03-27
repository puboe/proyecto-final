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
#print(conv)
#in_np = np.zeros((WIDTH, HEIGHT), np.float32)
image_filenames = sys.argv[1:]
image_filenames.sort()
print(image_filenames)

s_np = [np.array(Image.open(f), np.float32, copy=True)/255.0 - 0.5
        for f in image_filenames]

#s_np = np.array(Image.open(sys.argv[2]))
#s_np = np.array(s_np, np.float32, copy=True)/255.0
#print(s_np.shape)

ishape = s_np[0].shape


conv_dim = 21

s_g = [cl.Buffer(ctx, mf.READ_ONLY | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=s) for s in s_np]

#conv = np.full((ishape[0], ishape[1], conv_dim**2 + 1), 1/(conv_dim**2), np.float32)
#conv = np.random.rand(ishape[0], ishape[1], conv_dim**2 + 1).astype(np.float32)/(conv_dim**2+1)
conv = np.zeros((ishape[0], ishape[1], conv_dim**2 + 1))
conv_g = cl.Buffer(ctx, mf.READ_WRITE | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=conv)

o_np = np.zeros(ishape, np.float32)
o_g = cl.Buffer(ctx, mf.READ_WRITE | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=o_np)

print('Loaded, start training.')
start_time = time.time()
#prg.enumerate(queue, res_np.shape, None, res_g, np.int32(res_np.shape[0]), np.int32(res_np.shape[1]))
prev_error = None
eta = 0.08
for x in range(200):
    if (x % 1 == 0 and x != 0):
        cl.enqueue_copy(queue, o_np, o_g)
        error = np.linalg.norm(o_np-s_np[len(s_np)-1])
        print(error)
        print('epoch', x)
        if prev_error is None:
            prev_error = error
        if error > prev_error:
            eta *= 0.95
        else:
            eta *= 1.0001
        prev_error = error
            
    for s_idx in range(len(s_np)-1):
        prg.nconvolve(queue, ishape, (10, 10), s_g[s_idx], o_g,
                      np.int32(ishape[0]), np.int32(ishape[1]),
                      conv_g, np.int32(conv_dim), np.int32(conv_dim))
        prg.adjust(queue, ishape, (10, 10), s_g[s_idx], o_g, s_g[s_idx+1],
                   np.int32(ishape[0]), np.int32(ishape[1]),
                   conv_g, np.int32(conv_dim), np.int32(conv_dim),
                   np.float32(eta))

cl.enqueue_copy(queue, conv, conv_g)
np.save('net', conv)
    #prg.hconvolve(queue, res_np.shape, (16, 16), in_g, res_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]), conv_g, np.int32(7), np.int32(7))
    #prg.hconvolve(queue, res_np.shape, (16, 16), res_g, in_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]), conv_g, np.int32(7), np.int32(7))
    ##cl.enqueue_copy(queue, in_g, res_g)
    
    #prg.gauss(queue, res_np.shape, (16,16), in_g, res_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]),np.int32(3), np.int32(3))
    #prg.gauss(queue, res_np.shape, (16,16), res_g, in_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]),np.int32(3), np.int32(3))

#prg.gauss(queue, res_np.shape, (16,16), in_g, res_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]),np.int32(3), np.int32(3))

#prg.nconvolve(queue, res_np.shape, (16, 16), in_g, res_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]), conv_g, np.int32(3), np.int32(3))

#for s_idx in range(len(s_np)):
#    cl.enqueue_copy(queue, s_np[s_idx], s_g[s_idx])
cl.enqueue_copy(queue, o_np, o_g)
print(np.linalg.norm(o_np))
print(np.linalg.norm(s_np[-1]))
print(o_np)
print('Done mafaka')
print(time.time() - start_time)
plt.imshow(s_np[-1] - o_np, interpolation='none', cmap='gray')
plt.show()

prg.nconvolve(queue, ishape, (10, 10), s_g[-2], o_g,
              np.int32(ishape[0]), np.int32(ishape[1]),
              conv_g, np.int32(conv_dim), np.int32(conv_dim))

cl.enqueue_copy(queue, o_np, o_g)

plt.imshow(o_np, interpolation='none', cmap='gray')
plt.show()

shutil.rmtree('result')
os.mkdir('result')

for image_filename in image_filenames:
    shutil.copyfile(image_filename, 'result/' + os.path.basename(image_filename))

result = Image.fromarray(((o_np+0.5) * 255).astype(np.uint8))
result.save('result/z.bmp')
