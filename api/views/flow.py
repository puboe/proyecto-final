from flask import jsonify
from api import app, db
from meteo.meteo_sql import MeteoState, MeteoMotionData, MeteoFlux
from PIL import Image, ImageDraw
from api.util import render_image
import numpy as np

@app.route('/<zone_name>/<datetime:start_time>/<datetime:end_time>/')
def show_flow(zone_name, start_time, end_time):
    states = db.session.query(MeteoState).filter_by(zone_name=zone_name) \
             .filter(MeteoState.time >= start_time) \
             .filter(MeteoState.time <= end_time) \
             .filter(MeteoMotionData.suitable_state()) \
             .order_by(MeteoState.time.asc()) \
             .all()
    return jsonify([state.time for state in states])

@app.route('/<zone_name>/<datetime:start_time>/<datetime:end_time>/trails')
def show_flow_trails(zone_name, start_time, end_time):
    states = db.session.query(MeteoState) \
             .filter_by(zone_name=zone_name) \
             .filter(MeteoState.time >= start_time) \
             .filter(MeteoState.time <= end_time) \
             .filter(MeteoMotionData.suitable_state()) \
             .order_by(MeteoState.time.asc()) \
             .all()
    state_times = [state.time for state in states]
    motions = db.session.query(MeteoMotionData) \
              .filter_by(zone_name=zone_name, method='gradient') \
              .filter(MeteoMotionData.prev_time.in_(state_times)) \
              .filter(MeteoMotionData.next_time.in_(state_times)) \
              .order_by(MeteoMotionData.prev_time.asc())

    #motions = [db.session.query(MeteoMotionData) \
               #.filter_by(prev_state=prev_state,
                          #next_state=next_state,
                          #method='gradient')
               #.first()
                    #for prev_state, next_state in zip(states[:-1], states[1:])]

    flux = MeteoFlux(motions)
    trail = flux.trail(flux.generate_start(15.0, 15.0), transpose=False)
    trail = flux.trim_noisy_trails(trail)
    trail = np.transpose(trail, (1, 0, 2))

    height, width = flux.shape

    image = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    im = image
    draw = ImageDraw.Draw(image)
    #return str(trail[10].reshape((trail[10].size,)))
    #for t in trail:
        #draw.line(t.reshape((t.size,)).tolist(), fill=(0, 0, 255, 64), width=3)
    for t in trail:
        for pstart, pend in zip(t[:-1], t[1:]):
            parallel = pend - pstart
            normal = np.array([-parallel[1], parallel[0]])
            if np.linalg.norm(normal) > 0.0:
                normal = 2.0*normal/np.linalg.norm(normal)
                a = pstart + normal
                b = pstart - normal
                c = pend
                draw.polygon(a.tolist() + b.tolist() + c.tolist(), fill=(0, 0, 255, 64))
    del draw
    return render_image(image)
