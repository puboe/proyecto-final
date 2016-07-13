from flask import jsonify, send_file, abort
from api import blueprint, db
from api.util import render_image_array
from meteo.meteo_sql import MeteoPredictionData


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
    data = db.session.query(MeteoPredictionData).filter_by(zone_name=zone_name,
                                                       time=time).first()
    if data is None:
        abort(404)

    return render_image_array(data.image)
