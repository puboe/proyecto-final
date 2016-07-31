from .connector import session_scope
from .meteo_sql import *
from . import image
import pyopencl
import sys

import datetime
import numpy as np


def calculate_prediction_data(state, flux):
    end_time = flux.timedelta.seconds // 60
    extrapolation_time = (state.time - flux.start_time).seconds // 60
    print('end_time', end_time)
    print('extrapolation_time', extrapolation_time)
    trail = np.transpose(flux.polyfitted_trails([end_time, extrapolation_time] , flux.generate_start(10.0, 10.0), 2), (2, 0, 1))
    trail = flux.trim_noisy_trails(trail)
    trail = np.transpose(trail, (1, 0, 2))

    prediction = np.zeros(flux.shape)
    count = np.zeros_like(prediction)

    for t in trail:
        pass

    print(trail)

    prediction = MeteoPredictionData(state=state)
    return prediction


with session_scope() as session:
    state = session.query(MeteoState).order_by(MeteoState.time.desc()).first()
    end_time = state.time - datetime.timedelta(hours=1)
    start_time = end_time - datetime.timedelta(hours=4)
    flux = MeteoFlux.from_interval(session, state.zone.name, start_time, end_time)
    prediction = calculate_prediction_data(state, flux)
    session.rollback()
    



#with session_scope() as session:
        #query = session.query(MeteoState).filter(MeteoMotionData.suitable_state())
        #print('State count', query.count())
        #query = query.filter(~MeteoState.predictions.any())
        #print('Predictionless count', query.count())
        #query = query.order_by(MeteoState.time)
        #predictionless = query.all()



        
