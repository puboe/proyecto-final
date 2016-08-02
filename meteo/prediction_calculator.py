from .connector import session_scope
from .meteo_sql import *
from . import image
import pyopencl
import sys

import datetime
import numpy as np


def calculate_prediction_data(state, flux):
    print('flux st', flux.start_time)
    print('flux et', flux.end_time)
    print('flux motions', flux.motions)
    end_time = flux.timedelta.seconds // 60
    extrapolation_time = (state.time - flux.start_time).seconds // 60
    print('end_time', end_time)
    print('extrapolation_time', extrapolation_time)
    trail = np.transpose(flux.polyfitted_trails([end_time, extrapolation_time] , flux.generate_start(15.0, 15.0), 2), (2, 0, 1))

    #trail = flux.trim_noisy_trails(trail, factor=4)
    trail = np.transpose(trail, (1, 0, 2))

    base = next((data for data in flux.states[-1].datas if data.channel == 'ir4'))

    prediction = np.zeros(base.image.shape)
    count = np.zeros_like(prediction)

    dx, dy = (20, 20)

    prediction[:,:] = base.image[:,:]*0.1


    for t in trail.astype(np.int):
        #(ex, ey), (sx, sy) = t
        (sx, sy), (ex, ey) = t


        if (ey-dy >= 0 and ex-dx >= 0 and ex+dx < base.image.shape[1] and ey+dy < base.image.shape[0]) and (sy-dy >= 0 and sx-dx >= 0 and sx+dx < base.image.shape[1] and sy+dy < base.image.shape[0]):
            #print((ex,ey),(sx,sy))
            prediction[ey-dy:ey+dy, ex-dx:ex+dx] += base.image[sy-dy:sy+dy, sx-dx:sx+dx]
            #prediction[ey-dy:ey+dy, ex-dx:ex+dx] = 1.0
            #prediction[sy-dy:sy+dy, sx-dx:sx+dx] = 0.0
            count[ey-dy:ey+dy, ex-dx:ex+dx] += 1.0
    prediction = prediction/(count + 0.1)

    comp = next((data for data in state.datas if data.channel == 'ir4'))

    print(np.sum(np.abs(prediction-comp.image)))

    prediction_data = MeteoPredictionData(state=state, image=prediction) if len(state.predictions) == 0 else state.predictions[0]

    prediction_data.image = prediction

    print(base.time)

    return prediction_data



with session_scope() as session:
    #state = session.query(MeteoState).order_by(MeteoState.time.desc()).first()
    for state in session.query(MeteoState).order_by(MeteoState.time.desc()):
        end_time = state.time - datetime.timedelta(hours=2)
        start_time = end_time - datetime.timedelta(hours=6)
        print('start_time', start_time)
        print('end_time', end_time)
        flux = MeteoFlux.from_interval(session, state.zone.name, start_time, end_time, 'gradient')
        prediction = calculate_prediction_data(state, flux)
    #session.rollback()
    



#with session_scope() as session:
        #query = session.query(MeteoState).filter(MeteoMotionData.suitable_state())
        #print('State count', query.count())
        #query = query.filter(~MeteoState.predictions.any())
        #print('Predictionless count', query.count())
        #query = query.order_by(MeteoState.time)
        #predictionless = query.all()



        
