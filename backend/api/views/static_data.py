from flask import jsonify, send_file
from api import blueprint, db
from api.util import render_image_array
from meteo.meteo_sql import MeteoStaticData, MeteoBackgroundData
import numpy as np


@blueprint.route('/<zone_name>/<datetime:time>/static/<satellite>/<channel>/')
def static_data(zone_name, time, satellite, channel):
    data = db.session.query(MeteoStaticData).filter_by(zone_name=zone_name,
                                                       time=time,
                                                       satellite=satellite,
                                                       channel=channel).first()
    if data is None:
        abort(404)

    data_dict = dict(time=data.state.time,
                     zone_name=data.state.zone.name,
                     satellite=data.satellite,
                     channel=data.channel,
                     size=data.image.shape)
    return jsonify(data_dict)

@blueprint.route('/<zone_name>/<datetime:time>/static/<satellite>/<channel>/image.png')
def static_data_image(zone_name, time, satellite, channel):
    data = db.session.query(MeteoStaticData).filter_by(zone_name=zone_name,
                                                       time=time,
                                                       satellite=satellite,
                                                       channel=channel).first()
    if data is None:
        abort(404)

    return render_image_array(data.image)

@blueprint.route('/<zone_name>/<datetime:time>/static/<satellite>/<channel>/image_enhanced.png')
def static_data_image_enhanced(zone_name, time, satellite, channel):
    data = db.session.query(MeteoStaticData).filter_by(zone_name=zone_name,
                                                       time=time,
                                                       satellite=satellite,
                                                       channel=channel).first()
    if data is None:
        abort(404)

    background = db.session.query(MeteoBackgroundData).filter_by(zone_name=zone_name,
                                                                 satellite=satellite,
                                                                 channel=channel).first()

    image = data.image - background.image
    image[image < 0.0] = 0.0
    image = image/np.max(image)
    return render_image_array(image)
