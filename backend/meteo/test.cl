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
