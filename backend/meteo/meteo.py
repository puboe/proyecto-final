import numpy as np
import time
from PIL import Image
import sys
import os
import shutil
import pickle
from functools import reduce
from datetime import datetime
from collections import namedtuple
from svg import SVGImage

import logging
import loggingtools
logging.basicConfig(stream=sys.stderr, level=logging.DEBUG)
logger = logging.getLogger("Meteo")
timefunction = loggingtools.create_timefunction(logger)
logblock = loggingtools.create_logblock(logger)


DEFAULT_CONFIG = {
        'crop_rect': (180, 160, 500, 300),
        'search_area': (15, 15),
        'window': (33, 33),
        'downsample': (10, 10),
        'channels': ['ir4'],
        'satellites': ['goeseast']
        }

def arange2d(starts, stops, steps, dtype=None):
    x, y = [np.arange(start, stop, step)
            for start, stop, step in zip(starts, stops, steps)]
    return np.transpose([np.tile(x, len(y)), np.repeat(y, len(x))])

def crop_image(image, crop_rect = None):
    if crop_rect is None:
        return image
    (x, y, w, h) = crop_rect
    return np.array(image[y:y+h, x:x+w], np.float32, copy=False, order='F')

def path_walker(path):
    while path != '' and path != '/':
        path, field = os.path.split(path)
        yield field


class MeteoBase(object):
    _image_processor = None
    
class MeteoStaticData(MeteoBase):
    @classmethod
    def from_image(cls, image_file, crop_rect=None):
        image = np.array(Image.open(image_file), np.float32, copy=True, order='F')/255.0
        fields = path_walker(image_file)
        time = datetime.strptime(next(fields)[:10], "%y%m%d%H%M")
        channel = next(fields)
        zone_name = next(fields)
        satellite = next(fields)

        return cls(time, zone_name, satellite, channel, crop_image(image, crop_rect))

    def __init__(self, time, zone, satellite, channel, image=None):
        self.time = time
        self.zone = zone
        self.satellite = satellite
        self.channel = channel
        self.image = image
        self._edges = None

    def _edges(self, processor):
        processor = processor or self._image_processor
        return processor.gradient(self.image)
    
    def edges(self, processor=None):
        if self._edges is not None:
            self._edges = self._edges(processor)
        return self._edges

    def plain(self, processor=None):
        return self.image

    @property
    def shape(self):
        return self.image.shape

    @property
    def valid(self):
        return np.any(self.image > 0)


class MeteoState(MeteoBase):
    @classmethod
    def from_images(cls, image_files, time, crop_rect=None):
        datas = [MeteoStaticData.from_image(image_file, crop_rect=crop_rect)
                    for image_file in image_files]


        return cls(datas, time)

    def __init__(self, datas=None, time=None, prev_state=None, zone=None):
        self.datas = datas or []
        self.prev_state = prev_state
        self.next_state = None
        self.time = time or self.first_data.time
        self.motion_data = None
        self.zone = zone
        if self.prev_state is not None:
            if zone is not None and zone is not self.prev_state.zone:
                raise ValueError('Zone must be None or match previous state zone.')
            if self is not self.prev_state.next_state:
                if self.prev_state.next_state is None:
                    self.prev_state.next_state = self
                else:
                    raise ValueError('Previous state already has next state.')

            if self.shape != self.prev_state.shape:
                raise ValueError('Previous state must have same shape')

            self.zone = self.prev_state.zone
            self.motion_data = MeteoMotionData(self)
        if self.zone is None:
            raise ValueError('Zone not specified')
        if self.time is None:
            raise ValueError('No time specified')

    @property
    def first_data(self):
        return self.datas[0]

    @property
    def shape(self):
        return self.first_data.shape

    @property
    def valid(self):
        return all([d.valid for d in self.datas])

    #@property
    #def datas(self):
        #return []

class MeteoMotionData(MeteoBase):
    def __init__(self, state):
        self.state = state
        self.motion_x = None
        self.motion_y = None
        self.motion_x_ds = None
        self.motion_y_ds = None

        #self.density_data_key = 'edges'
        self.density_data_key = 'plain'

    @property
    def search_area(self):
        return self.state.zone.config['search_area']

    @property
    def window(self):
        return self.state.zone.config['window']

    @property
    def downsample(self):
        return self.state.zone.config['downsample']

    @property
    def prev_state(self):
        return self.state.prev_state

    @property
    def next_state(self):
        return self.state

    def _motion_data(self, processor):
        processor = processor or self._image_processor
        prev_image = getattr(self.prev_state.first_data, self.density_data_key)(processor)
        next_image = getattr(self.next_state.first_data, self.density_data_key)(processor)
        return processor.bma(prev_image, next_image, self.search_area, self.window)

    def motion_data(self, processor=None):
        if self.motion_x is None or self.motion_y is None:
            self.motion_x, self.motion_y = self._motion_data(processor)
        return self.motion_x, self.motion_y

    def _motion_data_ds(self, processor):
        processor = processor or self._image_processor
        motion_x, motion_y = self.motion_data(processor)
        return (processor.downsample(motion_x, self.downsample),
                processor.downsample(motion_y, self.downsample))

    def motion_data_ds(self, processor=None):
        if self.motion_x_ds is None or self.motion_y_ds is None:
            self.motion_x_ds, self.motion_y_ds = self._motion_data_ds(processor)
        return self.motion_x_ds, self.motion_y_ds

    def trail_step(self, pos, backwards = False):
        dx_ds, dy_ds = self.motion_data_ds()
        pos_index = tuple(np.transpose(np.fliplr(pos.astype(np.uint32)) // self.downsample))
        dpos = np.transpose(np.array([dy_ds[pos_index], dx_ds[pos_index]]))
        return pos - dpos if backwards else pos + dpos

    @property
    def timedelta(self):
        return self.next_state.time - self.prev_state.time

    @property
    def shape(self):
        return self.state.shape


#class MeteoFlux(MeteoBase):
#    @classmethod
#    def from_images(cls, search_area, window, downsample, image_files, crop_rect=None):
#        states = [MeteoState.from_images([image_file], crop_rect)
#                    for image_file in image_files]
#        return cls.from_states(states, search_area, window, downsample)
#
#    @classmethod
#    def from_states(cls, states, search_area, window, downsample):
#        states = list(sorted(list(filter(lambda s: s.valid, states)),
#                             key=lambda s: s.time))
#
#        if len(states) < 2:
#            raise Exception('Need at least two states to calculate steps')
#
#        steps = [MeteoStep(prev_state, next_state, search_area, window, downsample)
#                   for prev_state, next_state in zip(states[:-1], states[1:])]
#
#        return cls(steps)
#
#    def __init__(self, steps):
#        self.steps = steps
#
#    def get_states(self):
#        yield self.steps[0].prev_state
#        for step in self.steps:
#            yield step.next_state
#
#    @property
#    def shape(self):
#        return self.steps[0].shape
#
#    def trail(self, start, transpose = False, backwards = False):
#        if not backwards:
#            result = np.array(reduce(lambda slist, step: slist + [step.trail_step(slist[-1])],
#                                     self.steps, [start]))
#        else:
#            result = np.array(reduce(lambda slist, step: [step.trail_step(slist[0], backwards=True)] + slist,
#                                     self.steps[::-1], [start]))
#        return np.transpose(result, (1, 0, 2)) if transpose else result
#
#    @property
#    def times(self):
#        return np.cumsum([0.0] + [step.timedelta.seconds // 60
#                            for step in self.steps])
#
#    def polys(self, start, deg, backwards = False):
#        trails = self.trail(start, transpose = True, backwards = backwards)
#        t = self.times
#        return [(np.polyfit(t, c, deg) for c in np.transpose(trail))
#                for trail in trails]
#
#    def polyfitted_trails(self, tstart, tstop, tstep, start, deg, backwards = False):
#        polys = self.polys(start, deg, backwards = backwards)
#        return [(np.polyval(poly, np.arange(tstart, tstop, tstep)) for poly in polypair)
#                    for polypair in polys]


class MeteoZone(object):
    @classmethod
    def from_image(cls, map_image_file, name, config=None):
        map_image = np.array(Image.open(map_image_file), np.float32, copy=True, order='F')/255.0
        return cls(name, states=[], config=config, map_image=crop_image(map_image, config['crop_rect']))

    @classmethod
    def to_image(cls, mmap, image_file):
        image = Image.fromarray((mmap.map_image*255.0).astype(np.uint8))
        image.save(image_file)

    def __init__(self, name, states=[], config=None, map_image=None):
        self.config = config
        self.states = states
        self.name = name
        self.map_image = map_image



def main():
    import sys
    import image
    import pyopencl
    from matplotlib import pyplot as plt
    images = sys.argv[1:]

    processor = image.CLImageProcessor(pyopencl)
    MeteoStaticData._image_processor = processor
    MeteoStep._image_processor = processor

    (ds_x, ds_y) = (10, 10)
    images.sort()
    #crop_rect = None
    crop_rect = (180, 160, 500, 300)  
    flux = MeteoFlux.from_images((15, 15), (33, 33), (ds_x, ds_y), images, crop_rect = crop_rect)
    mmap = MeteoMap.from_image("map.png", crop_rect = crop_rect)

    print(flux.times)

    start = np.fliplr(arange2d((10.0, 10.0), flux.shape, (10.0, 10.0), dtype=np.float32) - 10.0)
    trail = flux.trail(start, backwards = True)

    for index, (state, points) in enumerate(zip(flux.get_states(), trail)):
        x, y = np.transpose(points)
        height, width = flux.shape
        plt.xlim([0, width])
        plt.ylim([0, height])
        plt.gca().invert_yaxis()
        plt.imshow(state.first_data.image, interpolation='none', cmap='gray')
        plt.plot(x, y, 'ro')
        plt.savefig('result/' + str(index) + '.png', format='png')
        plt.clf()

    plt.imshow(mmap.image, interpolation='none', cmap='gray')
    for (v, dv) in zip(trail[:-1], np.diff(trail, axis=0)):
        x, y = np.transpose(v)
        dx, dy = np.transpose(dv)
        plt.quiver(x, y, dx, -dy, scale=1.0, units='x', color='blue')

    plt.show()

    start_time = 0.0
    end_time = np.max(flux.times) + 60.0

    MeteoMap.to_image(mmap, "result/map.png")
    svg = SVGImage("map.png", "result/lines.svg", flux.shape)

    plt.imshow(mmap.image, interpolation='none', cmap='gray')
    for (tx, ty) in flux.polyfitted_trails(start_time, end_time, 5.0, start, 2):
        plt.plot(tx, ty, 'b')
        txy = list(zip(tx, ty))
        for v1, v2 in zip(txy[:-1], txy[1:]):
            svg.addLine(v1, v2)
    svg.save()


    plt.show()

if __name__ == "__main__":
    main()

