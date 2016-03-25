inline int _2dlin(const int w, const int h, const int x, const int y) {
    return h*x + y;
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
    /* int ex = sx + gw; */
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
        irow += h; ipos = irow;
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
    if (sx + cw > w) cw = w - sx;
    if (sy + ch > h) ch = h - sy;
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
    cpos = &convm[_2dlin(w*cw*ch, h*cw*ch, tx, ty)];
    // Check for image limits
    if (sx < 0) {cw += sx; sx = 0;}
    if (sy < 0) {ch += sy; sy = 0;}
    if (sx + cw > w) cw = w - sx;
    if (sy + ch > h) ch = h - sy;
    irow = &input[_2dlin(w, h, sx, sy)]; ipos = irow;
    // Process sensorial neurons
    for (int y = 0; y < ch; y++) {
        for (int x = 0; x < cw; x++) {
            result += (*cpos)*(*ipos);
            ipos++; cpos++;
        }
        irow += h; ipos = irow;
    }
    // Process bias neuron
    result += *cpos;
    output[_2dlin(w, h, tx, ty)] = result;
}
