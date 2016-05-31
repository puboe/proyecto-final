from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import *
from sqlalchemy.orm import relationship, deferred, column_property
from sqlalchemy.ext.hybrid import hybrid_property, hybrid_method
from meteo_sql_types import *

from contextlib import contextmanager


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


    #@hybrid_property
    #def is_valid(self):
        #return self.image is not None


class MeteoState(Base):
    __tablename__ = 'meteo_state'
    # Attributes
    time = Column(DateTime, primary_key=True, nullable=False)
    zone_name = Column(ForeignKey('meteo_zone.name'), primary_key=True, nullable=False)
    # Relationships
    zone = relationship('MeteoZone', backref='states')
    datas = relationship('MeteoStaticData', back_populates='state')


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


    @hybrid_property
    def timedelta(self):
        return self.next_state.time - self.prev_state.time

