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
    # TODO: Should fit zone dimensions in a regular field somewhere here
    __tablename__ = 'meteo_zone'
    NAME_TYPE = String(64)
    # Attributes
    name = Column(NAME_TYPE, primary_key=True)
    map_image = deferred(Column(NumpyArray, nullable=True))
    config = Column(JSONType, nullable=True)

class MeteoBackgroundData(Base):
    __tablename__ = 'meteo_background_data'
    # Attributes
    zone_name = Column(ForeignKey('meteo_zone.name'), nullable=False, primary_key=True)
    satellite = Column(String(32), nullable=True, primary_key=True)
    channel = Column(String(32), nullable=True, primary_key=True)
    image = Column(NumpyArray, nullable=True)
    is_valid = column_property(image.isnot(None))

    # Relationships
    zone = relationship('MeteoZone', backref='background_datas')
    image = deferred(image)

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
    predictions = relationship('MeteoPredictionData', back_populates='state')

    @hybrid_property
    def is_valid(self):
        return any(map(lambda d: d.is_valid, self.datas))

    @is_valid.expression
    def is_valid(cls):
        return cls.datas.any(is_valid=True)

class MeteoPredictionData(Base):
    __tablename__ = 'meteo_prediction_data'
    __table_args__ = (
            ForeignKeyConstraint(['time', 'zone_name'],
                                 ['meteo_state.time', 'meteo_state.zone_name']),
            )
    # Attributes
    time = Column(DateTime, nullable=False, primary_key=True )
    zone_name = Column(MeteoZone.NAME_TYPE, nullable=False, primary_key=True)

    image = Column(NumpyArray, nullable=True)
    is_valid = column_property(image.isnot(None))

    # Relationships
    state = relationship('MeteoState', back_populates='predictions')
    image = deferred(image)


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
    motion_x_ds = deferred(motion_x_ds, group='motion_ds')
    motion_y_ds = deferred(motion_y_ds, group='motion_ds')

    # Relationships
    prev_state = relationship('MeteoState', foreign_keys=[prev_time, zone_name], backref='next_motions')
    next_state = relationship('MeteoState', foreign_keys=[next_time, zone_name], backref='prev_motions')


    def calculate_motion(self, processor):
        if 'composite' in self.method:
            motion_x_ir4, motion_y_ir4 = self._calculate_motion(processor, 'ir4')
            motion_x_ir2, motion_y_ir2 = self._calculate_motion(processor, 'ir2')
            self.motion_x = motion_x_ir4/2.0 + motion_x_ir2/2.0
            self.motion_y = motion_y_ir4/2.0 + motion_y_ir2/2.0
        else:
            self.motion_x, self.motion_y = self._calculate_motion(processor, 'ir4')

    def _calculate_motion(self, processor, channel):
        prev_image = next(filter(lambda d: d.channel == channel, self.prev_state.datas)).image
        next_image = next(filter(lambda d: d.channel == channel, self.next_state.datas)).image
        if 'back' in self.method:
            background = next(filter(lambda d: d.channel == channel, self.prev_state.zone.background_datas)).image
            prev_image = prev_image - background
            next_image = next_image - background
        if 'gradient' in self.method:
            prev_image = processor.gradient(prev_image)
            next_image = processor.gradient(next_image)
        search_area = self.prev_state.zone.config['search_area']
        window = self.prev_state.zone.config['window']
        motion_x, motion_y = processor.bma(prev_image, next_image,
                                                     search_area, window)
        return motion_x, motion_y

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
        # TODO: Shape should be taken from an accesible field, not the image.
        #       Currently using the downsampled array as it's faster
        # return self.motion_x.shape
        return np.array(self.motion_x_ds.shape) * self.downsample

    @property
    def downsample(self):
        return self.prev_state.zone.config['downsample']

    @classmethod
    def suitable_state(cls):
        return MeteoState.datas.any(is_valid=True, channel='ir4')



class MeteoFlux(object):
    @classmethod
    def from_interval(cls, session, zone_name, start_time, end_time, method):
        states = session.query(MeteoState) \
                     .filter_by(zone_name=zone_name) \
                     .filter(MeteoState.time >= start_time) \
                     .filter(MeteoState.time <= end_time) \
                     .filter(MeteoMotionData.suitable_state()) \
                     .order_by(MeteoState.time.asc()) \
                     .all()
        return cls.from_states(session, states, method)

    @classmethod
    def from_prev_states(cls, session, zone_name, end_time, steps, method):
        states = session.query(MeteoState).filter_by(zone_name=zone_name) \
                   .filter(MeteoState.time <= end_time) \
                   .filter(MeteoMotionData.suitable_state()) \
                   .order_by(MeteoState.time.desc()) \
                   .limit(steps) \
                   .all()
        states.reverse()
        return cls.from_states(session, states, method)


    @classmethod
    def from_states(cls, session, states, method):
        state_times = [state.time for state in states]
        zone_name = states[0].zone.name
        motions = session.query(MeteoMotionData) \
                  .filter_by(zone_name=zone_name, method=method) \
                  .filter(MeteoMotionData.prev_time.in_(state_times)) \
                  .filter(MeteoMotionData.next_time.in_(state_times)) \
                  .order_by(MeteoMotionData.prev_time.asc()) \
                  .all()
        return cls(motions)


    def __init__(self, motions):
        self.motions = motions

    @property
    def shape(self):
        return self.motions[0].shape

    def _arange2d(self, starts, stops, steps, dtype=None):
        x, y = [np.arange(start, stop, step)
                for start, stop, step in zip(starts, stops, steps)]
        return np.transpose([np.tile(x, len(y)), np.repeat(y, len(x))])

    def generate_start(self, sx, sy):
        return np.fliplr(self._arange2d((0.0, 0.0), self.shape, (sx, sy), dtype=np.float32))

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

    @property
    def timedelta(self):
        return self.end_time - self.start_time

    @property
    def start_time(self):
        return self.motions[0].prev_time

    @property
    def end_time(self):
        return self.motions[-1].next_time

    @property
    def states(self):
        return [self.motions[0].prev_state] + [motion.next_state for motion in self.motions]
    
    def smooth_times(self, minutes_interval):
        return np.arange(0.0, self.timedelta.seconds // 60, minutes_interval)

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

    def trim_noisy_trails(self, trails, factor=1.0):
        lengths = np.sum(np.linalg.norm(np.diff(trails, axis=0), axis=2), axis=0)
        mean = np.mean(lengths)
        std = np.std(lengths)
        return trails[:, lengths > mean + (std*factor)]

