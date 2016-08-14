from flask import jsonify, send_file, abort
from api import blueprint, db
from api.util import render_image_array
from meteo.meteo_sql import MeteoZone, MeteoStaticData, MeteoMotionData, MeteoFlux
from meteo.prediction import calculate_prediction_image
import numpy as np
import datetime


def get_prediction_image(zone_name, time, start_time, end_time):
    zone = db.session.query(MeteoZone) \
                     .filter_by(name=zone_name) \
                     .first()

    if zone is None:
        return None

    flux = MeteoFlux.from_interval(db.session,
                                   zone_name,
                                   start_time,
                                   end_time,
                                   zone.config['default_motion_method'])
    if len(flux.motions) < 1:
        return None

    base_data = db.session.query(MeteoStaticData) \
                          .filter_by(state=flux.states[-1]) \
                          .filter_by(channel='ir4') \
                          .first()
    pred_image = calculate_prediction_image(time, flux, base_data.image)
    return pred_image


@blueprint.route('/<zone_name>/<datetime:time>/prediction/image.png')
def prediction_data_image(zone_name, time):
    pred_image = get_prediction_image(zone_name,
                                      time,
                                      time - datetime.timedelta(hours=6),
                                      time - datetime.timedelta(hours=2))
    if pred_image is None:
        abort(404)

    return render_image_array(pred_image)

@blueprint.route('/<zone_name>/<datetime:time>/prediction/diff_image.png')
def prediction_data_image_difference(zone_name, time):
    state_data = db.session.query(MeteoStaticData).filter_by(is_valid=True,
                                                             zone_name=zone_name,
                                                             channel='ir4',
                                                             time=time).first()
    pred_image = get_prediction_image(zone_name,
                                      time,
                                      time - datetime.timedelta(hours=6),
                                      time - datetime.timedelta(hours=2))

    if pred_image is None or state_data is None:
        abort(404)

    difference = np.abs(state_data.image - pred_image)
    difference = difference/np.max(difference)

    difference_rgb = np.dstack((difference, np.zeros_like(difference), 1.0 - difference))

    return render_image_array(difference_rgb)
