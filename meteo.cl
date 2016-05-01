__constant float gradient_convm[3] = {-1.0, 0.0, 1.0};

inline int _2dlin(const int w, const int h, const int x, const int y) {
    return x + y*w;
}
inline bool bounded(const int w, const int h, const int x, const int y) {
    return x >= 0 && y >= 0 && x < w && y < h;
}
inline bool rbounded(const int w, const int h,
                     const int x, const int y,
                     const int rw, const int rh) {
    return (x >= 0 && y >= 0 && x + rw < w && y + rh < h);
}

#define _DEFINE_CONVOLVE(__convm_addr_space, __name) \
  inline float __name(int tx, int ty, __global const float *input, __global float *output, const int w, const int h, __convm_addr_space const float * convm, int cw, int ch) { \
      float result = 0.0; \
      int sx = tx - cw/2, sy = ty - ch/2; \
      __global float * irow, * ipos; \
      __convm_addr_space const float * cpos = convm; \
      if (!rbounded(w, h, sx, sy, cw, ch)) \
          return 0.0; \
      irow = &input[_2dlin(w, h, sx, sy)]; ipos = irow; \
      for (int y = 0; y < ch; y++) { \
          for (int x = 0; x < cw; x++) { \
              result += (*cpos)*(*ipos); \
              ipos++; cpos++; \
          } \
          irow += w; ipos = irow; \
      } \
      return result; \
  }

_DEFINE_CONVOLVE(__global, _global_convolve)
_DEFINE_CONVOLVE(__constant, _constant_convolve)

__kernel void convolve(__global const float *input, __global float *output, const int w, const int h, __global const float * convm, int cw, int ch) {
    int tx = get_global_id(0), ty = get_global_id(1);
    output[_2dlin(w, h, tx, ty)] = _global_convolve(tx, ty,
                                                    input, output, w, h,
                                                    convm, cw, ch);
}

__kernel void cconvolve(__global const float *input, __global float *output, const int w, const int h, __global const float * convm, int cw, int ch) {
    int tx = get_global_id(0), ty = get_global_id(1);
    __global const float * cpos = &convm[_2dlin(w*cw*ch, h*cw*ch, tx, ty)];
    output[_2dlin(w, h, tx, ty)] = _global_convolve(tx, ty,
                                                    input, output, w, h,
                                                    cpos, cw, ch);
}

__kernel void gradient(__global const float *input, __global float *output) {
    int tx = get_global_id(0), ty = get_global_id(1);
    int w = get_global_size(0), h = get_global_size(1);
    float dx = _constant_convolve(tx, ty,
                                  input, output, w, h,
                                  gradient_convm, 3, 1);
    float dy = _constant_convolve(tx, ty,
                                  input, output, w, h,
                                  gradient_convm, 1, 3);
    output[_2dlin(w, h, tx, ty)] = sqrt(dx*dx + dy*dy);
}

inline float image_distance(const int aw, const int ah, const int ax, const int ay, __global const float * im_a, const int bw, const int bh, const int bx, const int by, __global const float * im_b, const int w, const int h) {
    float result = 0.0;
    float perror = 0.0;
    float im_a_val, im_b_val;
    float a_total = 0.0;
    for (int x = 0; x < w; x++)
        for(int y = 0; y < h; y++) {
            im_a_val = im_a[_2dlin(aw, ah, ax + x, ay + y)];
            im_b_val = im_b[_2dlin(bw, bh, bx + x, by + y)];
            //perror = im_a_val*(im_a_val - im_b_val);
            //result += perror*perror;
            //a_total += im_a_val;
            perror = im_a_val - im_b_val;
            result += perror*perror;
        }
    return result; //a_total;
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

__kernel void bma(__global float *prev, __global float *next, __global float * out_dx, __global float * out_dy, const int mdx, const int mdy, const int bw, const int bh) {
    int rw = get_global_size(0);
    int rh = get_global_size(1);
    int rx = get_global_id(0);
    int ry = get_global_id(1);
    int w = rw*bw;
    int h = rh*bh;
    int cx = rx*bw;
    int cy = ry*bh;
    float best_dx = 0.0, best_dy = 0.0, best_distance = INFINITY;
    float distance = 0.0;
    if (!bounded(w, h, cx - mdx - bw/2, cy - mdy - bh/2) ||
        !bounded(w, h, cx + mdx + bw/2, cy + mdy + bh/2)) {
        out_dx[_2dlin(rw, rh, rx, ry)] = 0.0;
        out_dy[_2dlin(rw, rh, rx, ry)] = 0.0;
        return;
    }
    for (int dx = (cx - mdx); dx <= (cx + mdx); dx++)
        for (int dy = (cy - mdy); dy <= (cy + mdy); dy++) {
            distance = image_distance(w, h, cx - bw/2, cy - bh/2, prev,
                                      w, h, dx - bw/2, dy - bh/2, next,
                                      bw, bh);//*100000 + (cx -dx)*(cx-dx) + (cy-dy)*(cy-dy);
            if (distance < best_distance) {
                best_distance = distance;
                best_dx = (dx - cx);
                best_dy = (dy - cy);
            }
        }
    out_dx[_2dlin(rw, rh, rx, ry)] = best_dx;
    out_dy[_2dlin(rw, rh, rx, ry)] = best_dy;
}

__kernel void best_delta(__global const float *prev, __global const float *next, __global float * dx, __global float * dy, const int cw, const int ch, const int aw, const int ah) {
    int w = get_global_size(0);
    int h = get_global_size(1);
    int cx = get_global_id(0);
    int cy = get_global_id(1);
    int sx = cx - cw/2;
    int sy = cy - ch/2;
    float best_dx = 0.0, best_dy = 0.0, best_distance = INFINITY;
    float distance = 0.0;
    if (!rbounded(w, h, sx - aw/2 , sy - ah/2 , cw + aw, ch + ah)) {
        dx[_2dlin(w, h, cx, cy)] = 0.0;
        dy[_2dlin(w, h, cx, cy)] = 0.0;
        return;
    }
    for (int x = sx; x < (sx + cw); x++)
        for (int y = sy; y < (sy + ch); y++) {
            distance = image_distance(w, h, cx - aw/2, cy - ah/2, prev,
                                      w, h, x - aw/2, y - ah/2, next,
                                      aw, ah);
            if (distance < best_distance || 
                (distance == best_distance && (x == cx && y == cy))) {
                  
                best_distance = distance;
                best_dx = (x - cx);
                best_dy = (y - cy);
            }
        }
    dx[_2dlin(w, h, cx, cy)] = best_dx;//*prev[_2dlin(w, h, cx, cy)]*10;
    dy[_2dlin(w, h, cx, cy)] = best_dy;//*prev[_2dlin(w, h, cx, cy)]*10;
}
__kernel void downsample2d(__global float *dest, __global const float *source, const int ds_x, const int ds_y) {
    int dest_x = get_global_id(0);
    int dest_y = get_global_id(1);
    int w = get_global_size(0)*ds_x;
    int h = get_global_size(1)*ds_y;
    int source_x = dest_x*ds_x;
    int source_y = dest_y*ds_y;
    float result = 0.0;
    for (int x = source_x; x < source_x + ds_x; x++)
        for (int y = source_y; y < source_y + ds_y; y++)
            result += source[_2dlin(w, h, x, y)];
    //dest[_2dlin(w/ds_x, h/ds_y, dest_x, dest_y)] = source[_2dlin(w, h, source_x, source_y)];
    dest[_2dlin(w/ds_x, h/ds_y, dest_x, dest_y)] = result/(ds_x*ds_y);
}
