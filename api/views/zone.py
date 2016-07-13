from flask import jsonify, redirect, url_for, send_file, request, abort
from sqlalchemy import cast, String, Date
from api import app, db
from api.util import request_wants_json, render_image_array
from meteo.meteo_sql import MeteoZone, MeteoState
from PIL import Image
import numpy as np
import io

@app.route('/<zone_name>/')
def show_zone(zone_name):
    zone = db.session.query(MeteoZone).filter_by(name=zone_name).first()
    if zone is None:
        abort(404)
    if request_wants_json():
        query = db.session.query(MeteoState).filter_by(zone=zone, is_valid=True)
        first_state = query.order_by(MeteoState.time.asc()).first()
        last_state = query.order_by(MeteoState.time.desc()).first()
        return jsonify(dict(
                        name=zone.name,
                        config=zone.config,
                        first_state_time=first_state.time,
                        last_state_time=last_state.time
                    ))
    else:
        last_state = db.session.query(MeteoState) \
                        .filter_by(zone=zone) \
                        .filter(MeteoState.is_valid) \
                        .order_by(MeteoState.time.desc()).first()
        return redirect(url_for('show_state',
                                zone_name=last_state.zone.name,
                                time=last_state.time))

@app.route('/<zone_name>/map_image.png')
def zone_map_image(zone_name):
    zone = db.session.query(MeteoZone).filter_by(name=zone_name).first()
    if zone is None:
        abort(404)
    return render_image_array(zone.map_image)

@app.route('/<zone_name>/search_state')
def zone_search_state(zone_name):
    term = request.args.get("term")
    search_terms = term.split(' ')
    query = db.session.query(MeteoState).filter_by(zone_name=zone_name) \
                                        .filter_by(is_valid=True)
    for search_term in search_terms:
        query = query.filter(cast(MeteoState.time, String).ilike('%' + search_term + '%'))
    query = query.order_by(MeteoState.time)
    return jsonify([state.time for state in query.all()])


@app.route('/<zone_name>/states')
def zone_valid_states(zone_name):
    zone = db.session.query(MeteoZone).filter_by(name=zone_name).first()
    states = db.session.query(MeteoState) \
                .filter_by(zone_name=zone_name) \
                .filter(MeteoState.is_valid).all()
    state_times = [state.time for state in states]
    if zone is not None:
        return jsonify(state_times)
    else:
        abort(404)
