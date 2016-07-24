from .connector import session_scope
from .meteo_sql import *
from . import image
import pyopencl
import sys


def calculate_prediction_data(state, flux):
    prediction = MeteoPredictionData(state=state)
    return prediction


with session_scope() as session:
    flux = MeteoFlux.from_interval(session, start_time, end_time)
    prediction = calculate_prediction_data(state, flux)
    



#with session_scope() as session:
        #query = session.query(MeteoState).filter(MeteoMotionData.suitable_state())
        #print('State count', query.count())
        #query = query.filter(~MeteoState.predictions.any())
        #print('Predictionless count', query.count())
        #query = query.order_by(MeteoState.time)
        #predictionless = query.all()



        
