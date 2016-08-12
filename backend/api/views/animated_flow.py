from flask import jsonify
from api import db, blueprint, app
from meteo.meteo_sql import MeteoState, MeteoMotionData, MeteoFlux, MeteoStaticData
from PIL import Image, ImageDraw
from api.util import render_animation
import numpy as np

DEFAULT_INTERVAL = 0.2


def render_trails(array, points):
    height, width = array.shape
    image = Image.fromarray((array*255.0).astype(np.uint8), mode='L')
    draw = ImageDraw.Draw(image, mode='L')
    for x, y in points:
        draw.ellipse((x-1.0, y-1.0, x+1.0, y+1.0) , fill=0)
    return np.array(image)

@blueprint.route('/<zone_name>/<datetime:end_time>/flow/<int:steps>/<satellite>/<channel>/anim.gif')
def show_animated_flow(zone_name, end_time, steps, satellite, channel):
    datas = db.session.query(MeteoStaticData) \
                      .filter_by(zone_name=zone_name) \
                      .filter_by(satellite=satellite, channel=channel) \
                      .filter(MeteoStaticData.time <= end_time) \
                      .filter(MeteoMotionData.suitable_state()) \
                      .order_by(MeteoStaticData.time.desc()) \
                      .limit(steps) \
                      .all()
    datas.reverse()
    states = [data.state for data in datas]
    return render_animation([data.image for data in datas], DEFAULT_INTERVAL)


@blueprint.route('/<zone_name>/<datetime:end_time>/flow/<int:steps>/<satellite>/<channel>/anim_trails.gif')
def show_animated_trails(zone_name, end_time, steps, satellite, channel):
    datas = db.session.query(MeteoStaticData) \
                      .filter_by(zone_name=zone_name) \
                      .filter_by(satellite=satellite, channel=channel) \
                      .filter(MeteoStaticData.time <= end_time) \
                      .filter(MeteoMotionData.suitable_state()) \
                      .order_by(MeteoStaticData.time.desc()) \
                      .limit(steps) \
                      .all()
    datas.reverse()
    states = [data.state for data in datas]
    flux = MeteoFlux.from_states(db.session, states, states[0].zone.config['default_motion_method'])
    trail = flux.trail(flux.generate_start(10.0, 10.0))
    trail = flux.trim_noisy_trails(trail)

    trail_images = [render_trails(data.image, points)
                        for data, points in zip(datas, trail)]
    
    return render_animation(trail_images, DEFAULT_INTERVAL)
