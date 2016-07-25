from flask import jsonify
from api import db, blueprint
from meteo.meteo_sql import MeteoState, MeteoMotionData, MeteoFlux
from PIL import Image, ImageDraw
from api.util import render_image
import numpy as np

DEFAULT_STEPS = 6

@blueprint.route('/<zone_name>/<datetime:end_time>/flow/',
                 defaults=dict(steps=DEFAULT_STEPS))
@blueprint.route('/<zone_name>/<datetime:end_time>/flow/<int:steps>')
def show_fixed_flow(zone_name, end_time, steps):
    states = db.session.query(MeteoState).filter_by(zone_name=zone_name) \
             .filter(MeteoState.time <= end_time) \
             .filter(MeteoMotionData.suitable_state()) \
             .order_by(MeteoState.time.desc()) \
             .limit(steps) \
             .all()
    states.reverse()
    return jsonify([state.time for state in states])

@blueprint.route('/<zone_name>/<datetime:end_time>/flow/trails.png',
                 defaults=dict(steps=DEFAULT_STEPS))
@blueprint.route('/<zone_name>/<datetime:end_time>/flow/<int:steps>/trails.png')
def show_fixed_flow_trails(zone_name, end_time, steps):
    states = db.session.query(MeteoState).filter_by(zone_name=zone_name) \
             .filter(MeteoState.time <= end_time) \
             .filter(MeteoMotionData.suitable_state()) \
             .order_by(MeteoState.time.desc()) \
             .limit(steps) \
             .all()
    states.reverse()
    state_times = [state.time for state in states]
    motions = db.session.query(MeteoMotionData) \
              .filter_by(zone_name=zone_name, method='gradient') \
              .filter(MeteoMotionData.prev_time.in_(state_times)) \
              .filter(MeteoMotionData.next_time.in_(state_times)) \
              .order_by(MeteoMotionData.prev_time.asc()) \
              .all()

    #motions = [db.session.query(MeteoMotionData) \
               #.filter_by(prev_state=prev_state,
                          #next_state=next_state,
                          #method='gradient')
               #.first()
                    #for prev_state, next_state in zip(states[:-1], states[1:])]

    flux = MeteoFlux(motions)
    #trail = flux.trail(flux.generate_start(15.0, 15.0), transpose=False)
    trail = np.transpose(flux.polyfitted_trails(flux.smooth_times(60), flux.generate_start(10.0, 10.0), 2), (2, 0, 1))
    trail = flux.trim_noisy_trails(trail)
    trail = np.transpose(trail, (1, 0, 2))

    height, width = flux.shape

    ifactor = 2
    image = Image.new('RGB', (ifactor*width, ifactor*height), (0, 0, 0))
    imask = Image.new('RGB', image.size, (0, 0, 0))
    draw = ImageDraw.Draw(image, mode='RGBA')
    idraw = ImageDraw.Draw(imask, mode='RGBA')
    #return str(trail[10].reshape((trail[10].size,)))
    #for t in trail:
        #draw.line(t.reshape((t.size,)).tolist(), fill=(0, 0, 255, 64), width=3)
    trail = trail*ifactor
    for t in trail:
        for n, (pstart, pend) in enumerate(zip(t[:-1], t[1:])):
            parallel = pend - pstart
            normal = np.array([-parallel[1], parallel[0]])
            if np.linalg.norm(normal) > 0.0:
                normal = 2.5*ifactor*normal/np.linalg.norm(normal)
                a = pstart + normal
                b = pstart - normal
                c = pend
                draw.polygon(a.tolist() + b.tolist() + c.tolist(), fill=(0, 0, 255))
                idraw.polygon(a.tolist() + b.tolist() + c.tolist(), fill=(255, 255, 255, 52))
    del draw
    image.putalpha(imask.split()[0])
    #image = image.resize((width, height), Image.ANTIALIAS)
    return render_image(image)



@blueprint.route('/<zone_name>/<datetime:end_time>/flow/arrows.png',
                 defaults=dict(steps=DEFAULT_STEPS))
@blueprint.route('/<zone_name>/<datetime:end_time>/flow/<int:steps>/arrows.png')
def show_fixed_flow_arrows(zone_name, end_time, steps):
    states = db.session.query(MeteoState).filter_by(zone_name=zone_name) \
             .filter(MeteoState.time <= end_time) \
             .filter(MeteoMotionData.suitable_state()) \
             .order_by(MeteoState.time.desc()) \
             .limit(steps) \
             .all()
    states.reverse()
    state_times = [state.time for state in states]
    motions = db.session.query(MeteoMotionData) \
              .filter_by(zone_name=zone_name, method='gradient') \
              .filter(MeteoMotionData.prev_time.in_(state_times)) \
              .filter(MeteoMotionData.next_time.in_(state_times)) \
              .order_by(MeteoMotionData.prev_time.asc()) \
              .all()

    #motions = [db.session.query(MeteoMotionData) \
               #.filter_by(prev_state=prev_state,
                          #next_state=next_state,
                          #method='gradient')
               #.first()
                    #for prev_state, next_state in zip(states[:-1], states[1:])]

    flux = MeteoFlux(motions)
    #trail = flux.trail(flux.generate_start(15.0, 15.0), transpose=False)
    minutes = flux.timedelta.seconds // 60
    trail = np.transpose(flux.polyfitted_trails([minutes*0.33, minutes*0.66, minutes], flux.generate_start(20.0, 20.0), 2), (2, 0, 1))
    trail = flux.trim_noisy_trails(trail, 1.5)
    trail = np.transpose(trail, (1, 0, 2))

    height, width = flux.shape

    ifactor = 2
    image = Image.new('RGB', (ifactor*width, ifactor*height), (0, 0, 0))
    imask = Image.new('RGB', image.size, (0, 0, 0))
    draw = ImageDraw.Draw(image, mode='RGBA')
    idraw = ImageDraw.Draw(imask, mode='RGBA')
    #return str(trail[10].reshape((trail[10].size,)))
    #for t in trail:
        #draw.line(t.reshape((t.size,)).tolist(), fill=(0, 0, 255, 64), width=3)
    trail = trail*ifactor
    for t in trail:
        for n, (pstart, pend) in enumerate(zip(t[:-1], t[1:])):
            parallel = pend - pstart
            normal = np.array([-parallel[1], parallel[0]])
            if np.linalg.norm(normal) > 0.0:
                normal = 5.0*ifactor*normal/np.linalg.norm(normal)
                a = pstart + normal
                b = pstart - normal
                c = pend
                draw.polygon(a.tolist() + b.tolist() + c.tolist(), fill=(128, 0, 0))
                idraw.polygon(a.tolist() + b.tolist() + c.tolist(), fill=(255, 255, 255, 148))
    del draw
    image.putalpha(imask.split()[0])
    #image = image.resize((width, height), Image.ANTIALIAS)
    return render_image(image)

