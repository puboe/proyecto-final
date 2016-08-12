from flask import jsonify
from api import db, blueprint, app
from meteo.meteo_sql import MeteoState, MeteoMotionData, MeteoFlux, MeteoZone
from PIL import Image, ImageDraw
from api.util import render_image
import numpy as np

@blueprint.route('/<zone_name>/<datetime:start_time>/<datetime:end_time>/')
def show_flow(zone_name, start_time, end_time):
    states = db.session.query(MeteoState).filter_by(zone_name=zone_name) \
             .filter(MeteoState.time >= start_time) \
             .filter(MeteoState.time <= end_time) \
             .filter(MeteoMotionData.suitable_state()) \
             .order_by(MeteoState.time.asc()) \
             .all()
    return jsonify([state.time for state in states])

@blueprint.route('/<zone_name>/<datetime:start_time>/<datetime:end_time>/trails.png')
def show_flow_trails(zone_name, start_time, end_time):
    zone = db.session.query(MeteoZone).filter_by(name=zone_name).first()
    method = zone.config['default_motion_method']
    flux = MeteoFlux.from_interval(db.session, zone_name, start_time, end_time, method)
    #trail = flux.trail(flux.generate_start(15.0, 15.0), transpose=False)
    trail = np.transpose(flux.polyfitted_trails(flux.smooth_times(30), flux.generate_start(10.0, 10.0), 2), (2, 0, 1))
    trail = flux.trim_noisy_trails(trail)
    trail = np.transpose(trail, (1, 0, 2))

    height, width = flux.shape

    image = Image.new('RGBA', (4*width, 4*height), (0, 0, 0, 0))
    im = image
    draw = ImageDraw.Draw(image)
    #return str(trail[10].reshape((trail[10].size,)))
    #for t in trail:
        #draw.line(t.reshape((t.size,)).tolist(), fill=(0, 0, 255, 64), width=3)
    trail = trail*4
    for t in trail:
        for pstart, pend in zip(t[:-1], t[1:]):
            parallel = pend - pstart
            normal = np.array([-parallel[1], parallel[0]])
            if np.linalg.norm(normal) > 0.0:
                normal = 8.0*normal/np.linalg.norm(normal)
                a = pstart + normal
                b = pstart - normal
                c = pend
                draw.polygon(a.tolist() + b.tolist() + c.tolist(), fill=(0, 0, 255, 64))
    del draw
    #image = image.resize((width, height), Image.ANTIALIAS)
    return render_image(image)
