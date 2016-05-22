from sqlalchemy import *

import code

metadata = MetaData()

meteo_data = Table('meteo_data', metadata,
                   Column('id', Integer, primary_key=True),
                   Column('satellite', String(32), nullable=False),
                   Column('zone', String(32), nullable=False),
                   Column('type', String(32), nullable=False),
                   Column('data', LargeBinary, nullable=True),
                   Column('state', ForeignKey('meteo_state.id'), nullable=True))

meteo_state = Table('meteo_state', metadata,
                    Column('id', Integer, primary_key=True),
                    Column('time', Integer, nullable=False))

meteo_step = Table('meteo_step', metadata,
                   Column('id', Integer, primary_key=True),
                   Column('prev_state', ForeignKey('meteo_state.id'), nullable=False),
                   Column('post_state', ForeignKey('meteo_state.id'), nullable=False),
                   Column('dx', LargeBinary, nullable=True),
                   Column('dy', LargeBinary, nullable=True))

if __name__ == '__main__':
    #engine = create_engine('mysql+mysqldb://piedpiper:olakase@bananastic.ddns.net/piedpiper', pool_recycle=3600)
    engine = create_engine('postgresql+psycopg2://piedpiper:piedpiper@exp/piedpiper')
    #code.interact(local=locals())
    metadata.create_all(engine)
