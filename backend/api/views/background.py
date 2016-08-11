from flask import jsonify, send_file
from api import blueprint, db
from api.util import render_image_array
from meteo.meteo_sql import MeteoBackgroundData


@blueprint.route('/<zone_name>/background/<satellite>/<channel>/image.png')
def background_data_image(zone_name, satellite, channel):
    data = db.session.query(MeteoBackgroundData).filter_by(zone_name=zone_name,
                                                           satellite=satellite,
                                                           channel=channel).first()
    if data is None:
        abort(404)

    return render_image_array(data.image)
