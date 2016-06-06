from flask import Flask, jsonify, abort, send_file, render_template, redirect, url_for, request
from meteo.meteo_sql import MeteoStaticData, MeteoState, MeteoMotionData, MeteoZone
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import cast, String

from datetime import datetime
import dateutil.parser
import io
import numpy as np
from PIL import Image

app =  Flask(__name__)
#app.config['SQLALCHEMY_DATABASE_URI'] = 'postgresql+psycopg2://piedpiper:piedpiper@exp/piedpiper'
app.config['SQLALCHEMY_DATABASE_URI'] = 'postgresql+psycopg2://piedpiper:piedpiper@gserver/piedpiper'
db = SQLAlchemy(app)

@app.route('/')
def root():
    first_zone = db.session.query(MeteoZone).first()
    if first_zone is None:
        abort(404)
    return redirect(url_for('show_zone', zone_name=first_zone.name))

@app.route('/<zone_name>/')
def show_zone(zone_name):
    zone = db.session.query(MeteoZone).filter_by(name=zone_name).first()
    if zone is None:
        abort(404)
    last_state = db.session.query(MeteoState) \
                    .filter_by(zone=zone) \
                    .filter(MeteoState.is_valid) \
                    .order_by(MeteoState.time.desc()).first()
    return redirect(url_for('show_state',
                            zone_name=last_state.zone.name,
                            timestr=last_state.time.isoformat()))


def get_related_states(state):
    prev_state = db.session.query(MeteoState) \
                           .filter_by(is_valid=True, zone=state.zone) \
                           .filter(MeteoState.time < state.time) \
                           .order_by(MeteoState.time.desc()) \
                           .first()
    next_state = db.session.query(MeteoState) \
                           .filter_by(is_valid=True, zone=state.zone) \
                           .filter(MeteoState.time > state.time) \
                           .order_by(MeteoState.time.asc()) \
                           .first()
    first_state = db.session.query(MeteoState) \
                           .filter_by(is_valid=True, zone=state.zone) \
                           .order_by(MeteoState.time.asc()) \
                           .first()
    last_state = db.session.query(MeteoState) \
                           .filter_by(is_valid=True, zone=state.zone) \
                           .order_by(MeteoState.time.desc()) \
                           .first()

    return dict(first_state=first_state,
                prev_state=prev_state,
                next_state=next_state,
                last_state=last_state)


@app.route('/<zone_name>/<timestr>/')
def state_data(zone_name, timestr):
    state = db.session.query(MeteoState) \
                      .filter_by(zone_name=zone_name, time=dateutil.parser.parse(timestr)) \
                      .first()
    if state is None:
        abort(404)
    return render_template('state_data.html', state=state,
                                     target_state_view='state_data',
                                     **get_related_states(state))


@app.route('/<zone_name>/<timestr>/flow')
def state_flow(zone_name, timestr):
    state = db.session.query(MeteoState) \
                      .filter_by(zone_name=zone_name, time=dateutil.parser.parse(timestr)) \
                      .first()
    if state is None:
        abort(404)
    return render_template('state_flow.html', state=state,
                                     target_state_view='state_flow',
                                     **get_related_states(state))

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

@app.route('/<zone_name>/search_state')
def zone_search_state(zone_name):
    term = request.args.get("term")
    search_terms = term.split(' ')
    query = db.session.query(MeteoState).filter_by(zone_name=zone_name) \
                                        .filter_by(is_valid=True)
    for search_term in search_terms:
        query = query.filter(cast(MeteoState.time, String).ilike('%' + search_term + '%'))
    query = query.order_by(MeteoState.time)
    return jsonify([state.time.isoformat() for state in query.all()])

@app.route('/<zone_name>/map_image.png')
def zone_map_image(zone_name):
    zone = db.session.query(MeteoZone).filter_by(name=zone_name).first()
    if zone is not None:
        image = Image.fromarray((zone.map_image*255.0).astype(np.uint8))
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
    time = dateutil.parser.parse(time)
    data = db.session.query(MeteoStaticData).filter_by(zone_name=zone_name,
                                                       time=dateutil.parser.parse(timestr),
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
                                                       time=dateutil.parser.parse(timestr),
                                                       satellite=satellite,
                                                       channel=channel).first()
    if data is not None:
        image = Image.fromarray((data.image*255.0).astype(np.uint8))
        output = io.BytesIO()
        image.save(output, format='PNG')
        return send_file(io.BytesIO(output.getvalue()), mimetype='image/png')
    else:
        abort(404)