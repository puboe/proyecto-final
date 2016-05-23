from sqlalchemy import *
from sqlalchemy.orm import mapper, sessionmaker, relationship, deferred

import code
import pickle

from meteo import MeteoState, MeteoStep, MeteoData, MeteoZone


class NumpyArray(TypeDecorator):
    """
        Adapt numpy arrays to LargeBinary (blobs)
    """
    impl = LargeBinary

    def process_bind_param(self, value, dialect):
        if value is not None:
            value = pickle.dumps(value)

        return value

    def process_result_value(self, value, dialect):
        if value is not None:
            value = pickle.loads(value)
        return value

metadata = MetaData()

meteo_raw_data = Table('meteo_raw_data', metadata,
                   Column('id', Integer, primary_key=True),
                   Column('time', Date, nullable=False),
                   Column('zone', String(64), nullable=False),
                   Column('satellite', String(32), nullable=False),
                   Column('channel', String(32), nullable=False),
                   Column('image', NumpyArray, nullable=False),
                   UniqueConstraint('time', 'zone', 'satellite', 'channel'))

meteo_data = Table('meteo_data', metadata,
                   Column('id', Integer, primary_key=True),
                   Column('satellite', String(32), nullable=False),
                   Column('channel', String(32), nullable=False),
                   Column('image', NumpyArray, nullable=True),
                   Column('state_id', ForeignKey('meteo_state.id'), nullable=True),
                   UniqueConstraint('satellite', 'channel', 'state_id'))

meteo_state = Table('meteo_state', metadata,
                    Column('id', Integer, primary_key=True),
                    Column('time', Date, nullable=False),
                    Column('zone_name', ForeignKey('meteo_zone.name'), nullable=True),
                    UniqueConstraint('time', 'zone_name'))

meteo_zone = Table('meteo_zone', metadata,
                   Column('name', String(64), primary_key=True),
                   Column('crop_x', Integer, nullable=True),
                   Column('crop_y', Integer, nullable=True),
                   Column('crop_w', Integer, nullable=True),
                   Column('crop_h', Integer, nullable=True),
                   Column('search_area_w', Integer, nullable=True),
                   Column('search_area_h', Integer, nullable=True),
                   Column('window_w', Integer, nullable=True),
                   Column('window_h', Integer, nullable=True))

meteo_step = Table('meteo_step', metadata,
                   Column('id', Integer, primary_key=True),
                   Column('prev_state_id', ForeignKey('meteo_state.id'), nullable=False),
                   Column('next_state_id', ForeignKey('meteo_state.id'), nullable=False),
                   Column('motion_x', NumpyArray, nullable=True),
                   Column('motion_y', NumpyArray, nullable=True),
                   Column('motion_x_ds', NumpyArray, nullable=True),
                   Column('motion_y_ds', NumpyArray, nullable=True),
                   UniqueConstraint('prev_state_id', 'next_state_id'))


mapper(MeteoData, meteo_data, properties= {
    'image': deferred(meteo_data.c.image)
})

mapper(MeteoState, meteo_state, properties={
    'datas': relationship(MeteoData, order_by=meteo_data.c.channel)
})

mapper(MeteoStep, meteo_step, properties={
    'prev_state': relationship(meteo_state, foreign_keys='prev_state_id'),
    'next_state': relationship(meteo_state, foreign_keys='next_state_id'),
    'motion_x': deferred(meteo_step.c.motion_x, group='motion'),
    'motion_y': deferred(meteo_step.c.motion_y, group='motion'),
    'motion_x_ds': deferred(meteo_step.c.motion_x_ds, group='motion_ds'),
    'motion_y_ds': deferred(meteo_step.c.motion_y_ds, group='motion_ds')
})

mapper(MeteoZone, meteo_zone, properties={
    'states': relationship(MeteoState)
})

if __name__ == '__main__':
    #engine = create_engine('mysql+mysqldb://piedpiper:olakase@bananastic.ddns.net/piedpiper', pool_recycle=3600)
    engine = create_engine('postgresql+psycopg2://piedpiper:piedpiper@exp/piedpiper')
    metadata.create_all(engine)
    
    Session = sessionmaker(bind=engine)
    session = Session()
    code.interact(local=locals())

