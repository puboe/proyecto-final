from .connector import session_scope
from .meteo_sql import *
from . import image
import pyopencl
import sys

# Create missing motions
with session_scope() as session:
    for zone in session.query(MeteoZone).all():
        print('Zone', zone.name)
        
        query = session.query(MeteoState) \
                       .filter(MeteoMotionData.suitable_state()) \
                       .filter_by(zone=zone)

        print('State count', query.count())

        for method_name, method_config in zone.config['motion_methods'].items():
            if method_config['enabled']:
                motionless = query.filter(~MeteoState.next_motions.any(method=method_name)) \
                                  .order_by(MeteoState.time).all()
                print('Motionless count', len(motionless))
                motions = [MeteoMotionData(prev_state=prev_state, next_state=next_state, method=method_name)
                            for prev_state, next_state in zip(motionless[:-1], motionless[1:])]
                for motion in motions:
                    session.add(motion)


def get_next_motion(session):
    for zone in session.query(MeteoZone).all():
        for method_name, method_config in zone.config['motion_methods'].items():
            if method_config['enabled']:
                motion = session.query(MeteoMotionData) \
                                .filter_by(zone_name=zone.name) \
                                .filter_by(method=method_name) \
                                .filter_by(is_valid=False) \
                                .order_by(MeteoMotionData.prev_time.desc()) \
                                .first()
                if motion is not None:
                    return motion
    return None

# Calculate one motion
processor = image.CLImageProcessor(pyopencl)

motion_calculated = True
while motion_calculated:
    motion_calculated = False
    with session_scope() as session:
        print('Missing calculation', session.query(MeteoMotionData).filter_by(is_valid=False).count())

        motion = get_next_motion(session)
        if motion is not None:
            print(motion.prev_state.zone.name)
            print(motion.prev_state.time)
            print(motion.next_state.time)
            print(motion.prev_state.zone.config)
            motion.calculate_motion_ds(processor)
            motion_calculated = True
    



