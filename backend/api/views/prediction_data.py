from flask import jsonify, send_file, abort
from api import blueprint, db
from api.util import render_image_array
from meteo.meteo_sql import MeteoPredictionData, MeteoStaticData
import numpy as np


@blueprint.route('/<zone_name>/<datetime:time>/prediction/')
def prediction_data(zone_name, time):
    data = db.session.query(MeteoPredictionData).filter_by(zone_name=zone_name,
                                                       time=time).first()
    if data is None:
        abort(404)

    data_dict = dict(time=data.state.time,
                     zone_name=data.state.zone.name,
                     size=data.image.shape)
    return jsonify(data_dict)

@blueprint.route('/<zone_name>/<datetime:time>/prediction/image.png')
def prediction_data_image(zone_name, time):
    data = db.session.query(MeteoPredictionData).filter_by(zone_name=zone_name,
                                                       time=time).first()
    if data is None:
        abort(404)

    return render_image_array(data.image)

@blueprint.route('/<zone_name>/<datetime:time>/prediction/diff_image.png')
def prediction_data_image_difference(zone_name, time):
    prediction_data = db.session.query(MeteoPredictionData).filter_by(zone_name=zone_name,
                                                       time=time).first()
    state_data = db.session.query(MeteoStaticData).filter_by(is_valid=True,
                                                             zone_name=zone_name,
                                                             channel='ir4',
                                                             time=time).first()
    if prediction_data is None or state_data is None:
        abort(404)

    difference = np.abs(prediction_data.image - state_data.image)
    difference = difference/np.max(difference)

    difference_rgb = np.dstack((difference, np.zeros_like(difference), 1.0 - difference))

    return render_image_array(difference_rgb)
