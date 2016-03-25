#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import absolute_import, print_function
import numpy as np
import pyopencl as cl
import time


ctx = cl.create_some_context()
queue = cl.CommandQueue(ctx)


prg = cl.Program(ctx, """
inline int _2dlin(const int w, const int h, const int x, const int y) {
    return x + y*w;
}
inline bool bounded(const int w, const int h, const int x, const int y) {
    return x >= 0 && y >= 0 && x < w && y < h;
}
__kernel void enumerate(__global float *res_g, const int width, const int height) {
    int x = get_global_id(0);
    int y = get_global_id(1);
    int linear = _2dlin(width, height, x, y);
    res_g[linear] = x+10*y;
}
__kernel void gauss(__global const float *input, __global float *output, const int w, const int h, const int gw, const int gh) {
    int cx = get_global_id(0);
    int cy = get_global_id(1);
    float result = 0.0;
    float weight = 1.0/(gw*gh);
    int sx = cx - gw/2;
    int sy = cy - gh/2;
    int ex = cx + gw/2;
    int ey = cy + gh/2;
    //int ex = sx + gw;
    //int ey = sy + gh;
    if (sx < 0) sx = 0;
    if (sy < 0) sy = 0;
    if (ex >= w) ex = w-1;
    if (ey >= h) ey = h-1;
    for (int x = sx; x <= ex; x++)
        for (int y = sy; y <= ey; y++)
            result += input[_2dlin(w, h, x, y)];
    output[_2dlin(w, h, cx, cy)] = result*weight;
}
__kernel void convolve(__global const float *input, __global float *output, const int w, const int h, __global const float * convm, int cw, int ch) {
    int tx = get_global_id(0);
    int ty = get_global_id(1);
    float result = 0.0;
    int sx = tx - cw/2, sy = ty - ch/2;
    __global float * irow, * ipos;
    //__global float * crow, * cpos;
    __global float * cpos = convm;
    if (sx < 0) {cw += sx; sx = 0;}
    if (sy < 0) {ch += sy; sy = 0;}
    irow = &input[_2dlin(w, h, sx, sy)]; ipos = irow;
    //crow = &convm[0]; cpos = crow;
    if (sx + cw > w) cw = w - sx;
    if (sy + ch > h) ch = h - sy;
    for (int y = 0; y < ch; y++) {
        for (int x = 0; x < cw; x++) {
            result += (*cpos)*(*ipos);
            ipos++; cpos++;
        }
        irow += w; ipos = irow;
        //crow += cw; cpos = crow;
    }
    output[_2dlin(w, h, tx, ty)] = result;
}
__kernel void hconvolve(__global const float *input, __global float *output, const int w, const int h, __global const float * convm, int cw, int ch) {

    int tx = get_global_id(0);
    int ty = get_global_id(1);
    float result = 0.0;
    int sx = tx - cw/2, sy = ty - ch/2;
    __global float * irow, * ipos;
    __global float * cpos;
    cpos = &convm[_2dlin(w*cw*ch, h*cw*ch, tx, ty)];
    if (sx < 0) {cw += sx; sx = 0;}
    if (sy < 0) {ch += sy; sy = 0;}
    irow = &input[_2dlin(w, h, sx, sy)]; ipos = irow;
    //crow = &convm[0]; cpos = crow;
    if (sx + cw > w) cw = w - sx;
    if (sy + ch > h) ch = h - sy;
    for (int y = 0; y < ch; y++) {
        for (int x = 0; x < cw; x++) {
            result += (*cpos)*(*ipos);
            ipos++; cpos++;
        }
        irow += w; ipos = irow;
    }
    output[_2dlin(w, h, tx, ty)] = result;
}
""").build()

mf = cl.mem_flags
WIDTH  = 640
HEIGHT = 848
conv = np.full((WIDTH, HEIGHT, 3,3), 1.0/9.0, np.float32)
#print(conv)
conv_g = cl.Buffer(ctx, mf.READ_WRITE | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=conv)
in_np = np.zeros((WIDTH, HEIGHT), np.float32)
in_np[WIDTH/2][HEIGHT/2] = 1.0e12
#print(in_np)
#print(in_np[2][2])
print(in_np[150][150])
in_g = cl.Buffer(ctx, mf.READ_WRITE | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=in_np)
res_np = np.zeros(in_np.shape, np.float32)
res_g = cl.Buffer(ctx, mf.READ_WRITE | mf.ALLOC_HOST_PTR | mf.COPY_HOST_PTR, hostbuf=res_np)

#prg.enumerate(queue, res_np.shape, None, res_g, np.int32(res_np.shape[0]), np.int32(res_np.shape[1]))
for x in range(100):
    prg.hconvolve(queue, res_np.shape, None, in_g, res_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]), conv_g, np.int32(3), np.int32(3))
    prg.hconvolve(queue, res_np.shape, None, res_g, in_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]), conv_g, np.int32(3), np.int32(3))
    #cl.enqueue_copy(queue, in_g, res_g)

prg.convolve(queue, res_np.shape, None, in_g, res_g, np.int32(in_np.shape[0]), np.int32(in_np.shape[1]), conv_g, np.int32(3), np.int32(3))

cl.enqueue_copy(queue, res_np, res_g)
#print(res_np)
#print(res_np[2][2])
print(res_np[150][150])
