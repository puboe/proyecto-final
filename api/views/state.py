from api import app, db
from flask import Flask, jsonify, abort, send_file, render_template, redirect, url_for, request
from meteo.meteo_sql import MeteoStaticData, MeteoState, MeteoMotionData, MeteoZone, MeteoFlux
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import cast, String
from api.util import request_wants_json

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


@app.route('/<zone_name>/<datetime:time>/')
def show_state(zone_name, time):
    state = db.session.query(MeteoState) \
                      .filter_by(zone_name=zone_name,
                                 time=time,
                                 is_valid=True) \
                      .first()
    if state is None:
        abort(404)
    related_states = get_related_states(state)
    if request_wants_json():
        datas = [dict(
                        satellite=data.satellite,
                        channel=data.channel
                     )
                   for data in state.datas]
        prev_state = related_states.get('prev_state', None)
        next_state = related_states.get('next_state', None)
        prev_state_time = prev_state.time.isoformat() if prev_state else None
        next_state_time = next_state.time.isoformat() if next_state else None
        return jsonify(dict(
                        zone_name=state.zone.name,
                        time=state.time.isoformat(),
                        prev_state_time=prev_state_time,
                        next_state_time=next_state_time,
                        datas=datas
                      ))
    else:
        return render_template('state_data.html', state=state,
                                         target_state_view='show_state',
                                         **related_states)



@app.route('/<zone_name>/<datetime:time>/flow')
def state_flow(zone_name, time):
    state = db.session.query(MeteoState) \
                      .filter_by(zone_name=zone_name, time=time) \
                      .first()
    if state is None:
        abort(404)
    flow_states = db.session.query(MeteoState) \
                            .filter_by(zone=state.zone, is_valid=True) \
                            .filter(MeteoState.time < state.time) \
                            .order_by(MeteoState.time.desc()) \
                            .limit(10) \
                            .all()
    flow_states.reverse()

    motions = [db.session.query(MeteoMotionData) \
                         .filter_by(prev_state=prev_state,
                                    next_state=next_state,
                                    method='gradient') \
                         .first()
               for prev_state, next_state in zip(flow_states[:-1], flow_states[1:])]

    #trail = list(generate_flux_trail(motions, 10.0, 10.0))
    flux = MeteoFlux(motions)
    trail = flux.trail(flux.generate_start(10.0, 10.0))
    #trail = np.transpose(flux.polyfitted_trails(flux.times, flux.generate_start(10.0, 10.0), 6), (2, 0, 1))
    trail = flux.trim_noisy_trails(trail)
    #trail = list(map(lambda x: np.array(list(x)), trail))
    #raise Exception(str(list(map(list, trail))))
    #raise Exception(str(trail.shape) + ' ' + str(trail))

    flow_states_info = [dict(
                   time=fs.time.isoformat(),
                   image_url=url_for('static_data_image',
                                     zone_name=fs.zone.name,
                                     timestr=fs.time.isoformat(),
                                     satellite='goeseast',
                                     channel='ir4'),
                   marks=st.tolist()
                      )
                        for fs, st in zip(flow_states, trail)]
    return render_template('state_flow.html', state=state,
                                     target_state_view='state_flow',
                                     flow_states_info=flow_states_info,
                                     **get_related_states(state))




