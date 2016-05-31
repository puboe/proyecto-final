from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import *
from sqlalchemy.orm import sessionmaker, relationship, deferred, column_property
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

    @hybrid_method
    def has_valid_data(self, satellite=None, channel=None):
        datas = self.datas
        if satellite is not None:
            datas = filter(lambda d: d.satellite == satellite, datas)
        if channel is not None:
            datas = filter(lambda d: d.channel == channel, datas)
        return any(map(lambda d: d.is_valid, datas))

    @has_valid_data.expression
    def has_valid_data(cls, satellite=None, channel=None):
        query = cls.datas.any().where(MeteoStaticData.is_valid)
        #query = exists() \
                #.where(MeteoStaticData.time==MeteoState.time) \
                #.where(MeteoStaticData.zone_name==MeteoState.zone_name) \
                #.where(MeteoStaticData.is_valid)
        if satellite is not None:
            query = query.where(MeteoStaticData.satellite==satellite)
        if channel is not None:
            query = query.where(MeteoStaticData.channel==channel)
        return query

    @hybrid_property
    def has_motions(self):
        return 

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
    motion_x = deferred(Column(NumpyArray, nullable=True), group='motion')
    motion_y = deferred(Column(NumpyArray, nullable=True), group='motion')
    motion_x_ds = deferred(Column(NumpyArray, nullable=True), group='motion')
    motion_y_ds = deferred(Column(NumpyArray, nullable=True), group='motion')

    # Relationships
    prev_state = relationship('MeteoState', foreign_keys=[prev_time, zone_name], backref='next_motions')
    next_state = relationship('MeteoState', foreign_keys=[next_time, zone_name], backref='prev_motions')







    
_CONNECT_URI = 'postgresql+psycopg2://piedpiper:piedpiper@exp/piedpiper'
_ENGINE = None
_SESSION_MAKER = None

def get_engine(*args, connect_uri=_CONNECT_URI, **kwargs):
    global _ENGINE
    if _ENGINE is None:
        _ENGINE = create_engine(connect_uri)
    return _ENGINE

def get_session_maker(*args, **kwargs):
    global _SESSION_MAKER
    if _SESSION_MAKER is None:
        _SESSION_MAKER = sessionmaker(*args, bind=get_engine(*args, **kwargs), **kwargs)
    return _SESSION_MAKER

def get_session(*args, **kwargs):
    return get_session_maker(*args, **kwargs)()

@contextmanager
def session_scope():
    """Provide a transactional scope around a series of operations."""
    session = get_session()
    try:
        yield session
        session.commit()
    except:
        session.rollback()
        raise
    finally:
        session.close()

if __name__ == '__main__':
    import code
    Base.metadata.create_all(get_engine())
    Session = get_session_maker()
    session = Session()
    code.interact(local=locals())
