inline int _2dlin(const int w, const int h, const int x, const int y) {
    return h*x + y;
}
inline bool bounded(const int w, const int h, const int x, const int y) {
    return x >= 0 && y >= 0 && x < w && y < h;
}
inline bool rbounded(const int w, const int h,
                     const int x, const int y,
                     const int rw, const int rh) {
    return (x >= 0 && y >= 0 && x + rw < w && y + rh < h);
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
    if (!rbounded(w, h, sx, sy, gw, gh)) {
        output[_2dlin(w, h, cx, cy)] = input[_2dlin(w, h, cx, cy)];
        return;
    }
    for (int x = sx; x < (sx + gw); x++)
        for (int y = sy; y < (sy + gh); y++)
            result += input[_2dlin(w, h, x, y)];
    output[_2dlin(w, h, cx, cy)] = result*weight;
}
__kernel void convolve(__global const float *input, __global float *output, const int w, const int h, __global const float * convm, int cw, int ch) {
    int tx = get_global_id(0);
    int ty = get_global_id(1);
    float result = 0.0;
    int sx = tx - cw/2, sy = ty - ch/2;
    __global float * irow, * ipos;
    __global float * cpos = convm;
    if (!rbounded(w, h, sx, sy, cw, ch)) {
        output[_2dlin(w, h, tx, ty)] = input[_2dlin(w, h, tx, ty)];
        return;
    }
    irow = &input[_2dlin(w, h, sx, sy)]; ipos = irow;
    for (int y = 0; y < ch; y++) {
        for (int x = 0; x < cw; x++) {
            result += (*cpos)*(*ipos);
            ipos++; cpos++;
        }
        irow += h; ipos = irow;
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
    if (!rbounded(w, h, sx, sy, cw, ch)) {
        output[_2dlin(w, h, tx, ty)] = input[_2dlin(w, h, tx, ty)];
        return;
    }
    cpos = &convm[_2dlin(w*cw*ch, h*cw*ch, tx, ty)];
    irow = &input[_2dlin(w, h, sx, sy)]; ipos = irow;
    for (int y = 0; y < ch; y++) {
        for (int x = 0; x < cw; x++) {
            result += (*cpos)*(*ipos);
            ipos++; cpos++;
        }
        irow += h; ipos = irow;
    }
    output[_2dlin(w, h, tx, ty)] = result;
}
__kernel void nconvolve(__global const float *input, __global float *output, const int w, const int h, __global const float * convm, int cw, int ch) {

    int tx = get_global_id(0);
    int ty = get_global_id(1);
    float result = 0.0;
    int sx = tx - cw/2, sy = ty - ch/2;
    __global float * irow, * ipos;
    __global float * cpos;
    if (!rbounded(w, h, sx, sy, cw, ch)) {
        output[_2dlin(w, h, tx, ty)] = input[_2dlin(w, h, tx, ty)];
        return;
    }
    cpos = &convm[_2dlin(w*cw*ch, h*cw*ch, tx, ty)];
    irow = &input[_2dlin(w, h, sx, sy)]; ipos = irow;
    for (int y = 0; y < ch; y++) {
        for (int x = 0; x < cw; x++) {
            result += (*cpos)*(*ipos);
            ipos++; cpos++;
        }
        irow += h; ipos = irow;
    }
    result += *cpos;
    output[_2dlin(w, h, tx, ty)] = result/(cw*ch + 1);
}
__kernel void adjust(__global const float *input, __global const float *output, __global const float * sample, const int w, const int h, __global float * convm, int cw, int ch, const float eta) {
    int tx = get_global_id(0);
    int ty = get_global_id(1);
    int sx = tx - cw/2, sy = ty - ch/2;
    __global float * irow, * ipos;
    __global float * cpos;
    __global float * opos;
    float oval, sval;
    if (!rbounded(w, h, sx, sy, cw, ch))
        return;
    cpos = &convm[_2dlin(w*cw*ch, h*cw*ch, tx, ty)];
    irow = &input[_2dlin(w, h, sx, sy)]; ipos = irow;
    oval = output[_2dlin(w, h, tx, ty)];
    sval = sample[_2dlin(w, h, tx, ty)];
    // Adjust weights
    for (int y = 0; y < ch; y++) {
        for (int x = 0; x < cw; x++) {
            *cpos += 2.0*eta*(sval - oval)*(*ipos);
            ipos++; cpos++;
        }
        irow += h; ipos = irow;
    }
    // Adjust bias
    *cpos += 2.0*eta*(sval - oval);
}

inline float image_distance(const int aw, const int ah, const int ax, const int ay, __global const float * im_a, const int bw, const int bh, const int bx, const int by, __global const float * im_b, const int w, const int h) {
    float result = 0.0;
    float perror = 0.0;
    for (int x = 0; x < w; x++)
        for(int y = 0; y < h; y++) {
            perror = im_a[_2dlin(aw, ah, ax + x, ay + y)] -
                     im_b[_2dlin(bw, bh, bx + x, by + y)];
            result += perror*perror;
        }
    return result;
}

//__kernel void best_delta(__global const float *prev, __global const float *next, const int w, const int h, __global float * dx, __global float * dy, const int cw, const int ch, const int aw, const int ah) {
//    int cx = get_global_id(0);
//    int cy = get_global_id(1);
//    int sx = cx - cw/2;
//    int sy = cy - ch/2;
//    float avg_dx = 0.0, avg_dy = 0.0, total_fitness = 0.0;
//    float fitness;
//    if (!rbounded(w, h, sx - aw/2, sy - ch/2, cw + aw, ch + ah)) {
//        *dx = 0.0; *dy = 0.0;
//        return;
//    }
//    for (int x = sx; x < (sx + cw); x++)
//        for (int y = sy; y < (sy + ch); y++) {
//            fitness = 1.0/(1.0/(ah*ah*aw*aw) + image_distance(w, h, sx, sy, prev,
//                                      w, h, x - aw/2, y - ah/2, next,
//                                      aw, ah));
//            avg_dx += fitness*(x - cx);
//            avg_dy += fitness*(y - cy);
//            total_fitness += fitness;
//        }
//    dx[_2dlin(w, h, cx, cy)] = avg_dx/total_fitness;
//    dy[_2dlin(w, h, cx, cy)] = avg_dy/total_fitness;
//}

__kernel void best_delta(__global const float *prev, __global const float *next, const int w, const int h, __global float * dx, __global float * dy, const int cw, const int ch, const int aw, const int ah) {
    int cx = get_global_id(0);
    int cy = get_global_id(1);
    int sx = cx - cw/2;
    int sy = cy - ch/2;
    float best_dx = 0.0, best_dy = 0.0, best_distance = INFINITY;
    float distance = 0.0;
    if (!rbounded(w, h, sx - aw/2, sy - ch/2, cw + aw, ch + ah)) {
        *dx = 0.0; *dy = 0.0;
        return;
    }
    for (int x = sx; x < (sx + cw); x++)
        for (int y = sy; y < (sy + ch); y++) {
            distance = image_distance(w, h, cx - aw/2, cy - ah/2, prev,
                                      w, h, x - aw/2, y - ah/2, next,
                                      aw, ah);
            if (distance < best_distance || 
                (distance == best_distance && (x == cx && y == sy))) {
                  
                best_distance = distance;
                best_dx = (x - cx);
                best_dy = (y - cy);
            }
        }
    dx[_2dlin(w, h, cx, cy)] = best_dx;
    dy[_2dlin(w, h, cx, cy)] = best_dy;
}
__kernel void downsample2d(__global float *dest, __global const float *source, const int w, const int h, const int ds_x, const int ds_y) {
    int dest_x = get_global_id(0);
    int dest_y = get_global_id(1);
    int source_x = dest_x*ds_x;
    int source_y = dest_y*ds_y;
    float result = 0.0;
    for (int x = source_x; x < source_x + ds_x; x++)
        for (int y = source_y; y < source_y + ds_y; y++)
            result += source[_2dlin(w, h, x, y)];
    //dest[_2dlin(w/ds_x, h/ds_y, dest_x, dest_y)] = source[_2dlin(w, h, source_x, source_y)];
    dest[_2dlin(w/ds_x, h/ds_y, dest_x, dest_y)] = result/(ds_x*ds_y);


}
