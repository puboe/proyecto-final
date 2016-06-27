from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import *
from sqlalchemy.orm import relationship, deferred, column_property
from sqlalchemy.ext.hybrid import hybrid_property, hybrid_method
from .meteo_sql_types import *

from contextlib import contextmanager

import numpy as np
from functools import reduce


Base = declarative_base()


class MeteoZone(Base):
    __tablename__ = 'meteo_zone'
    NAME_TYPE = String(64)
    # Attributes
    name = Column(NAME_TYPE, primary_key=True)
    map_image = deferred(Column(NumpyArray, nullable=True))
    config = Column(JSONType, nullable=True)

class MeteoStaticData(Base):
    __tablename__ = 'meteo_static_data'
    __table_args__ = (
            #UniqueConstraint('time', 'zone_name', 'satellite', 'channel'),
            ForeignKeyConstraint(['time', 'zone_name'],
                                 ['meteo_state.time', 'meteo_state.zone_name']),
            )
    # Attributes
    time = Column(DateTime, nullable=False, primary_key=True )
    zone_name = Column(MeteoZone.NAME_TYPE, nullable=False, primary_key=True)
    satellite = Column(String(32), nullable=True, primary_key=True)
    channel = Column(String(32), nullable=True, primary_key=True)
    image = Column(NumpyArray, nullable=True)
    is_valid = column_property(image.isnot(None))

    # Relationships
    state = relationship('MeteoState', back_populates='datas')
    image = deferred(image)


class MeteoState(Base):
    __tablename__ = 'meteo_state'
    # Attributes
    time = Column(DateTime, primary_key=True, nullable=False)
    zone_name = Column(ForeignKey('meteo_zone.name'), primary_key=True, nullable=False)
    # Relationships
    zone = relationship('MeteoZone', backref='states')
    datas = relationship('MeteoStaticData', back_populates='state')

    @hybrid_property
    def is_valid(self):
        return any(map(lambda d: d.is_valid, self.datas))

    @is_valid.expression
    def is_valid(cls):
        return cls.datas.any(is_valid=True)


class MeteoMotionData(Base):
    __tablename__ = 'meteo_motion_data'
    __table_args__ = (
            #UniqueConstraint('prev_time', 'next_time', 'zone_name'),
            ForeignKeyConstraint(['prev_time', 'zone_name'],
                                 ['meteo_state.time', 'meteo_state.zone_name']),
            ForeignKeyConstraint(['next_time', 'zone_name'],
                                 ['meteo_state.time', 'meteo_state.zone_name'])
            )
    # Attributes
    prev_time = Column(DateTime, primary_key=True)
    next_time = Column(DateTime, primary_key=True)
    zone_name = Column(MeteoZone.NAME_TYPE, primary_key=True)
    method = Column(String(64), primary_key=True)
    motion_x = Column(NumpyArray, nullable=True)
    motion_y = Column(NumpyArray, nullable=True)
    motion_x_ds = Column(NumpyArray, nullable=True)
    motion_y_ds = Column(NumpyArray, nullable=True)
    is_valid = column_property(and_(motion_x.isnot(None),
                                    motion_y.isnot(None),
                                    motion_x_ds.isnot(None),
                                    motion_y_ds.isnot(None)))
    motion_x = deferred(motion_x, group='motion')
    motion_y = deferred(motion_y, group='motion')
    motion_x_ds = deferred(motion_x_ds, group='motion')
    motion_y_ds = deferred(motion_y_ds, group='motion')

    # Relationships
    prev_state = relationship('MeteoState', foreign_keys=[prev_time, zone_name], backref='next_motions')
    next_state = relationship('MeteoState', foreign_keys=[next_time, zone_name], backref='prev_motions')


    def calculate_motion(self, processor):
        prev_image = next(filter(lambda d: d.channel == 'ir4', self.prev_state.datas)).image
        next_image = next(filter(lambda d: d.channel == 'ir4', self.next_state.datas)).image
        if self.method == 'gradient':
            prev_image = processor.gradient(prev_image)
            next_image = processor.gradient(next_image)
        search_area = self.prev_state.zone.config['search_area']
        window = self.prev_state.zone.config['window']
        self.motion_x, self.motion_y = processor.bma(prev_image, next_image,
                                                     search_area, window)

    def calculate_motion_ds(self, processor):
        if self.motion_x is None or self.motion_y is None:
            self.calculate_motion(processor)
        self.motion_x_ds = processor.downsample(self.motion_x, self.downsample)
        self.motion_y_ds = processor.downsample(self.motion_y, self.downsample)


    def trail_step(self, pos, backwards = False):
        dx, dy = self.motion_x_ds, self.motion_y_ds
        pos_index = tuple(np.transpose(np.fliplr(pos.astype(np.uint32)) // self.downsample))
        dpos = np.transpose(np.array([dy[pos_index], dx[pos_index]]))
        return pos - dpos if backwards else pos + dpos

    #def trail_step(self, pos, backwards = False):
        #dx, dy = self.motion_x, self.motion_y
        #pos_index = tuple(np.transpose(np.fliplr(pos.astype(np.uint32))))
        #dpos = np.transpose(np.array([dy[pos_index], dx[pos_index]]))
        #return pos - dpos if backwards else pos + dpos

    @hybrid_property
    def timedelta(self):
        return self.next_state.time - self.prev_state.time

    @property
    def shape(self):
        return self.motion_x.shape

    @property
    def downsample(self):
        return self.prev_state.zone.config['downsample']

    @classmethod
    def suitable_state(cls):
        return MeteoState.datas.any(is_valid=True, channel='ir4')


def arange2d(starts, stops, steps, dtype=None):
    x, y = [np.arange(start, stop, step)
            for start, stop, step in zip(starts, stops, steps)]
    return np.transpose([np.tile(x, len(y)), np.repeat(y, len(x))])

#def generate_flux_trail(motions, sx, sy, transpose=False):
    #shape = motions[0].shape
    ##start = np.fliplr(arange2d((sx, sy), shape, (sx, sy), dtype=np.float32) - (sx, sy))
    #start = np.fliplr(arange2d((0.0, 0.0), shape, (sx, sy), dtype=np.float32))
    #result = np.array(reduce(lambda slist, step: slist + [step.trail_step(slist[-1])],
                                     #motions, [start]))
    #return np.transpose(result, (1, 0, 2)) if transpose else result
   

class MeteoFlux(object):
    def __init__(self, motions):
        self.motions = motions

    @property
    def shape(self):
        return self.motions[0].shape

    def generate_start(self, sx, sy):
        return np.fliplr(arange2d((0.0, 0.0), self.shape, (sx, sy), dtype=np.float32))

    def trail(self, start, transpose = False, backwards = False):
        if not backwards:
            result = np.array(reduce(lambda slist, step: slist + [step.trail_step(slist[-1])],
                                     self.motions, [start]))
        else:
            result = np.array(reduce(lambda slist, step: [step.trail_step(slist[0], backwards=True)] + slist,
                                     self.motions[::-1], [start]))
        return np.transpose(result, (1, 0, 2)) if transpose else result

    @property
    def times(self):
        return np.cumsum([0.0] + [motion.timedelta.seconds // 60
                            for motion in self.motions])

    def polys(self, start, deg, backwards = False):
        trails = self.trail(start, transpose = True, backwards = backwards)
        t = self.times
        return [[np.polyfit(t, c, deg) for c in np.transpose(trail)]
                for trail in trails]

    #def polyfitted_trails(self, tstart, tstop, tstep, start, deg, backwards = False):
        #polys = self.polys(start, deg, backwards = backwards)
        #return [(np.polyval(poly, np.arange(tstart, tstop, tstep)) for poly in polypair)
                    #for polypair in polys]

    def polyfitted_trails(self, times, start, deg, backwards = False):
        polys = self.polys(start, deg, backwards = backwards)
        return [np.array([np.polyval(poly, times) for poly in polypair])
                    for polypair in polys]

    def trim_noisy_trails(self, trails):
        return trails[:, np.sum(np.linalg.norm(np.diff(trails, axis=0), axis=2), axis=0) > 30 ]

