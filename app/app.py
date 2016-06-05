from flask import Flask, jsonify, abort, send_file, render_template, redirect, url_for
from meteo.meteo_sql import MeteoStaticData, MeteoState, MeteoMotionData, MeteoZone
from flask_sqlalchemy import SQLAlchemy

from datetime import datetime
import io
import numpy as np
from PIL import Image

app =  Flask(__name__)
#app.config['SQLALCHEMY_DATABASE_URI'] = 'postgresql+psycopg2://piedpiper:piedpiper@exp/piedpiper'
app.config['SQLALCHEMY_DATABASE_URI'] = 'postgresql+psycopg2://piedpiper:piedpiper@gserver/piedpiper'
db = SQLAlchemy(app)

def to_timestr(time):
    return time.strftime("%y%m%d%H%M")

def from_timestr(timestr):
    return datetime.strptime(timestr, "%y%m%d%H%M")


@app.route('/<zone_name>/')
def show_zone(zone_name):
    zone = db.session.query(MeteoZone).filter_by(name=zone_name).first()
    if zone is None:
        abort(404)
    last_state = db.session.query(MeteoState) \
                    .filter_by(zone=zone) \
                    .filter(MeteoState.is_valid) \
                    .order_by(MeteoState.time.desc()).first()
    return redirect(url_for('show_state', zone_name=last_state.zone.name, timestr=to_timestr(last_state.time)))

@app.route('/<zone_name>/<timestr>/')
def show_state(zone_name, timestr):
    zone = db.session.query(MeteoZone).filter_by(name=zone_name).first()
    if zone is None:
        abort(404)
    return render_template('map.html')

@app.route('/<zone_name>/info')
def zone(zone_name):
    zone = db.session.query(MeteoZone).filter_by(name=zone_name).first()
    if zone is not None:
        zone_attrs = dict(
                        name=zone.name,
                        config=zone.config
                        )
        return jsonify(zone_attrs)
    else:
        abort(404)

@app.route('/<zone_name>/map_image.png')
def zone_map_image(zone_name):
    zone = db.session.query(MeteoZone).filter_by(name=zone_name).first()
    if zone is not None:
        image_array = zone.map_image
        image = Image.fromarray((image_array*255.0).astype(np.uint8))
        output = io.BytesIO()
        image.save(output, format='PNG')
        return send_file(io.BytesIO(output.getvalue()), mimetype='image/png')
    else:
        abort(404)

@app.route('/<zone_name>/states')
def zone_valid_states(zone_name):
    states = db.session.query(MeteoState) \
                .filter_by(zone_name=zone_name) \
                .filter(MeteoState.is_valid).all()
    state_times = [state.time.isoformat() for state in states]
    if zone is not None:
        return jsonify(state_times)
    else:
        abort(404)

@app.route('/<zone_name>/<timestr>/static/<satellite>/<channel>/')
def static_data(zone_name, timestr, satellite, channel):
    time = from_timestr(time)
    data = db.session.query(MeteoStaticData).filter_by(zone_name=zone_name,
                                                       time=from_timestr(timestr),
                                                       satellite=satellite,
                                                       channel=channel).first()
    if data is not None:
        data_dict = dict(time=data.state.time, zone_name=data.state.zone.name, satellite=data.satellite, channel=data.channel, size=data.image.shape)
        return jsonify(data_dict)
    else:
        abort(404)

@app.route('/<zone_name>/<timestr>/static/<satellite>/<channel>/image.png')
def static_data_image(zone_name, timestr, satellite, channel):
    data = db.session.query(MeteoStaticData).filter_by(zone_name=zone_name,
                                                       time=from_timestr(timestr),
                                                       satellite=satellite,
                                                       channel=channel).first()
    if data is not None:
        image = Image.fromarray((data.image*255.0).astype(np.uint8))
        output = io.BytesIO()
        image.save(output, format='PNG')
        return send_file(io.BytesIO(output.getvalue()), mimetype='image/png')
    else:
        abort(404)
