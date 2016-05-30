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
    image = deferred(Column(NumpyArray, nullable=True))
    # Relationships
    state = relationship('MeteoState', backref='datas')

    @hybrid_property
    def is_valid(self):
        return self.image is not None


class MeteoState(Base):
    __tablename__ = 'meteo_state'
    # Attributes
    time = Column(DateTime, primary_key=True, nullable=False)
    zone_name = Column(ForeignKey('meteo_zone.name'), primary_key=True, nullable=False)
    # Relationships
    zone = relationship('MeteoZone', backref='states')

    is_valid = column_property(exists() \
                .where(MeteoStaticData.time==time) \
                .where(MeteoStaticData.zone_name==zone_name) \
                .where(MeteoStaticData.image.isnot(None)))

    prev_state = column_property(select(MeteoState) \
                                 .where(

    #@hybrid_property
    #def prev_state(self):
        #prev_states = list(filter(lambda s: s.time < self.time, self.zone.states))
        #return max(prev_states, key=lambda s: s.time) \
                #if len(prev_states) > 0 else None

    #@hybrid_property
    #def next_state(self):
        #next_states = list(filter(lambda s: s.time > self.time, self.zone.states))
        #return min(next_states, key=lambda s: s.time) \
                #if len(next_states) > 0 else None

    #@prev_state.expression
    #def prev_state(cls):
        #return select(MeteoState) \
               #.where(MeteoState.zone == self.zone) \
               #.where(MeteoState.time < self.time) \
               #.order_by(MeteoState.time)

    #@hybrid_property
    #def is_valid(self):
        #return any(map(lambda d: d.is_valid, self.datas))

    #@is_valid.expression
    #def is_valid(self):
        #return exists() \
                #.where(MeteoStaticData.time==MeteoState.time) \
                #.where(MeteoStaticData.zone_name==MeteoState.zone_name) \
                #.where(MeteoStaticData.image.isnot(None))


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
