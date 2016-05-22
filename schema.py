from sqlalchemy import *
from sqlalchemy.orm import mapper, sessionmaker, relationship, deferred

import code
import pickle

from meteo import MeteoState, MeteoData

class NumpyArray(TypeDecorator):
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

meteo_data = Table('meteo_data', metadata,
                   Column('id', Integer, primary_key=True),
                   Column('satellite', String(32), nullable=False),
                   Column('channel', String(32), nullable=False),
                   Column('image', NumpyArray, nullable=True),
                   Column('state', ForeignKey('meteo_state.id'), nullable=True))

meteo_state = Table('meteo_state', metadata,
                    Column('id', Integer, primary_key=True),
                    Column('time', Date, nullable=False),
                    Column('zone', ForeignKey('meteo_zone.id'), nullable=True))

meteo_zone = Table('meteo_zone', metadata,
                   Column('id', Integer, primary_key=True),
                   Column('name', String(64), nullable=False),
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
                   Column('prev', ForeignKey('meteo_state.id'), nullable=False),
                   Column('post', ForeignKey('meteo_state.id'), nullable=False),
                   Column('motion_x', NumpyArray, nullable=True),
                   Column('motion_y', NumpyArray, nullable=True),
                   Column('motion_x_ds', NumpyArray, nullable=True),
                   Column('motion_y_ds', NumpyArray, nullable=True))


mapper(MeteoData, meteo_data, properties= {
    'image': deferred(meteo_data.c.image)
})

mapper(MeteoState, meteo_state, properties={
    'datas': relationship(MeteoData, order_by=meteo_data.c.channel)
})

mapper(MeteoStep, meteo_step, properties={
    'motion_x': deferred(meteo_data.c.motion_x, group='motion'),
    'motion_y': deferred(meteo_data.c.motion_y, group='motion'),
    'motion_x_ds': deferred(meteo_data.c.motion_x_ds, group='motion_ds'),
    'motion_y_ds': deferred(meteo_data.c.motion_y_ds, group='motion_ds')
})

if __name__ == '__main__':
    #engine = create_engine('mysql+mysqldb://piedpiper:olakase@bananastic.ddns.net/piedpiper', pool_recycle=3600)
    engine = create_engine('postgresql+psycopg2://piedpiper:piedpiper@exp/piedpiper')
    metadata.create_all(engine)
    
    Session = sessionmaker(bind=engine)
    session = Session()
    code.interact(local=locals())

