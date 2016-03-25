#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import absolute_import, print_function
import numpy as np
import pyopencl as cl
import time

a_np = np.random.rand(10000000).astype(np.float32)
b_np = np.random.rand(10000000).astype(np.float32)

ctx = cl.create_some_context()
queue = cl.CommandQueue(ctx)


prg = cl.Program(ctx, """
float get_value2d(const float *array, const int width, const int height, const int * x, const int * y) {
    return array[x + width*y];
}
__kernel void enum(__global const float *res_g) {
    int gid = get_global_id(0);
    res_g[gid] = gid;
}
__kernel void sum(__global const float *a_g, __global const float *b_g, __global float *res_g) {
  int gid = get_global_id(0);
  res_g[gid] = pow(2.718281828459045, sin(a_g[gid] + b_g[gid]));
}
__kernel void mul(__global const float *a_g, __global const float *b_g, __global float *res_g) {
  int gid = get_global_id(0);
  res_g[gid] = a_g[gid]*b_g[gid];
}
__kernel void integrate(__global const float *a_g, __global const float *b_g, __global float *res_g) {
  int gid = get_global_id(0);
  res_g[gid] = (a_g[gid]*b_g[gid]);
}
""").build()

start_time = time.time()
mf = cl.mem_flags
a_g = cl.Buffer(ctx, mf.READ_ONLY | mf.USE_HOST_PTR, hostbuf=a_np)
b_g = cl.Buffer(ctx, mf.READ_ONLY | mf.USE_HOST_PTR, hostbuf=b_np)
res_g = cl.Buffer(ctx, mf.WRITE_ONLY, a_np.nbytes)

prg.sum(queue, a_np.shape, None, a_g, b_g, res_g)

res_np = np.empty_like(a_np)
cl.enqueue_copy(queue, res_np, res_g)
print(time.time() - start_time)

# Check on CPU with Numpy:
start_time = time.time()
result = res_np - np.power(np.e,np.sin(a_np + b_np))
print(time.time() - start_time)
print(result)
print(np.linalg.norm(result))
