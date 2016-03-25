#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import numpy as np
import pyopencl as cl
import pyopencl.array as cla
import time

a_np = np.random.rand(100000000).astype(np.float32)
b_np = np.random.rand(100000000).astype(np.float32)

ctx = cl.create_some_context()
q = cl.CommandQueue(ctx)

a_g = cla.to_device(q, a_np)
b_g = cla.to_device(q, b_np)

start_time = time.time()

r_g = cla.dot(a_g, b_g)
r_get = r_g.get()
print(time.time() - start_time)
start_time = time.time()
r_np = np.dot(a_np, b_np)
print(time.time() - start_time)

print(r_g.get() - (r_np))
