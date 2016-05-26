from contextlib import contextmanager

from sqlalchemy import *
from sqlalchemy.orm import mapper, sessionmaker, relationship, deferred, remote, foreign

import code
import pickle
import json

from meteo import MeteoState, MeteoMotionData, MeteoStaticData, MeteoZone
from meteo_raw import MeteoRawData


CONNECT_URI = 'postgresql+psycopg2://piedpiper:piedpiper@exp/piedpiper'
_ENGINE = None
_SESSION_MAKER = None

class JSONType(TypeDecorator):
    impl = String

    def process_bind_param(self, value, dialect):
        if value is not None:
            value = json.dumps(value)

        return value

    def process_result_value(self, value, dialect):
        if value is not None:
            value = json.loads(value)
        return value

class PickleType(TypeDecorator):
    impl = LargeBinary

    def process_bind_param(self, value, dialect):
        if value is not None:
            value = pickle.dumps(value)

        return value

    def process_result_value(self, value, dialect):
        if value is not None:
            value = pickle.loads(value)
        return value

class NumpyArray(PickleType):
    pass


metadata = MetaData()

meteo_static_data = Table('meteo_static_data', metadata,
                   Column('id', Integer, primary_key=True),
                   Column('time', DateTime, nullable=False),
                   Column('zone_name', ForeignKey('meteo_zone.name'), nullable=False),
                   Column('satellite', String(32), nullable=False),
                   Column('channel', String(32), nullable=False),
                   Column('image', NumpyArray, nullable=False),
                   UniqueConstraint('time', 'zone_name', 'satellite', 'channel'))

meteo_state = Table('meteo_state', metadata,
                    Column('id', Integer, primary_key=True),
                    Column('time', DateTime, nullable=False),
                    Column('zone_name', ForeignKey('meteo_zone.name'), nullable=False),
                    Column('next_state_id', ForeignKey('meteo_state.id'), nullable=True),
                    UniqueConstraint('time', 'zone_name'))

meteo_zone = Table('meteo_zone', metadata,
                   Column('name', String(64), primary_key=True),
                   Column('map_image', NumpyArray, nullable=True),
                   Column('config', JSONType, nullable=True))

meteo_motion_data = Table('meteo_motion_data', metadata,
                       Column('state_id', ForeignKey('meteo_state.id'), nullable=False, primary_key=True),
                       Column('motion_x', NumpyArray, nullable=True),
                       Column('motion_y', NumpyArray, nullable=True),
                       Column('motion_x_ds', NumpyArray, nullable=True),
                       Column('motion_y_ds', NumpyArray, nullable=True))


mapper(MeteoStaticData, meteo_static_data, properties= {
    'image': deferred(meteo_static_data.c.image),
    'zone': relationship(MeteoZone)
})

mapper(MeteoState, meteo_state, properties={
    'datas': relationship(MeteoStaticData, order_by=meteo_static_data.c.channel, primaryjoin=and_(meteo_state.c.time == foreign(meteo_static_data.c.time), meteo_state.c.zone_name == foreign(meteo_static_data.c.zone_name)) ),
    'motion_data': relationship(MeteoMotionData, uselist=False),
    'next_state': relationship(MeteoState, remote_side=meteo_state.c.id, back_populates='prev_state', lazy='joined', join_depth=1, uselist=False),
    'prev_state': relationship(MeteoState, back_populates='next_state', lazy='joined', join_depth=1, uselist=False)
})

mapper(MeteoMotionData, meteo_motion_data, properties={
    'state': relationship(MeteoState),
    'motion_x': deferred(meteo_motion_data.c.motion_x, group='motion'),
    'motion_y': deferred(meteo_motion_data.c.motion_y, group='motion'),
    'motion_x_ds': deferred(meteo_motion_data.c.motion_x_ds, group='motion_ds'),
    'motion_y_ds': deferred(meteo_motion_data.c.motion_y_ds, group='motion_ds')
})

mapper(MeteoZone, meteo_zone, properties={
    'states': relationship(MeteoState, backref='zone'),
    'datas': relationship(MeteoStaticData),
    'map_image': deferred(meteo_zone.c.map_image)
})


def get_engine():
    global _ENGINE
    if _ENGINE is None:
        _ENGINE = create_engine(CONNECT_URI)
    return _ENGINE

def get_session_maker(*args, **kwargs):
    global _SESSION_MAKER
    if _SESSION_MAKER is None:
        _SESSION_MAKER = sessionmaker(*args, bind=get_engine(), **kwargs)
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
    #engine = create_engine('mysql+mysqldb://piedpiper:olakase@bananastic.ddns.net/piedpiper', pool_recycle=3600)
    metadata.create_all(get_engine())
    Session = get_session_maker()
    session = Session()
    code.interact(local=locals())

