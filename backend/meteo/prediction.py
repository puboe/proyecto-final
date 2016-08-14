import numpy as np

TRAIL_SEPARATION = (15.0, 15.0)
WINDOW_SIZE = (20, 20)

def calculate_prediction_image(pred_time, flux, base_image):
    end_time = flux.timedelta.seconds // 60
    extrapolation_time = (pred_time - flux.start_time).seconds // 60
    trail = np.transpose(flux.polyfitted_trails([end_time, extrapolation_time] , flux.generate_start(*TRAIL_SEPARATION), 2), (2, 0, 1))

    #trail = flux.trim_noisy_trails(trail, factor=4)
    trail = np.transpose(trail, (1, 0, 2))

    pred_image = np.zeros(base_image.shape)
    count = np.zeros_like(pred_image)

    dx, dy = WINDOW_SIZE

    pred_image[:,:] = base_image[:,:]*0.1

    for t in trail.astype(np.int):
        (sx, sy), (ex, ey) = t


        if (ey-dy >= 0 and ex-dx >= 0 and ex+dx < base_image.shape[1] and ey+dy < base_image.shape[0]) and (sy-dy >= 0 and sx-dx >= 0 and sx+dx < base_image.shape[1] and sy+dy < base_image.shape[0]):
            pred_image[ey-dy:ey+dy, ex-dx:ex+dx] += base_image[sy-dy:sy+dy, sx-dx:sx+dx]
            count[ey-dy:ey+dy, ex-dx:ex+dx] += 1.0
    pred_image = pred_image/(count + 0.1)

    return pred_image
