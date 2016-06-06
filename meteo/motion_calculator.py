from .connector import session_scope
from .meteo_sql import *
from . import image
import pyopencl


# Create missing motions
with session_scope() as session:
    print('State count', session.query(MeteoState).filter_by(is_valid=True).count())
    motionless = session.query(MeteoState) \
                        .filter_by(is_valid=True) \
                        .filter(~MeteoState.next_motions.any()) \
                        .order_by(MeteoState.time).all()
    
    print('Motionless count', len(motionless))

    motions = [MeteoMotionData(prev_state=prev_state, next_state=next_state)
                for prev_state, next_state in zip(motionless[:-1], motionless[1:])]

    for motion in motions:
        session.add(motion)


# Calculate one motion
processor = image.CLImageProcessor(pyopencl)

motion_calculated = True
while motion_calculated:
    motion_calculated = False
    with session_scope() as session:
        print('Missing calculation', session.query(MeteoMotionData).filter_by(is_valid=False).count())
        motion = session.query(MeteoMotionData) \
                        .filter_by(is_valid=False) \
                        .order_by(MeteoMotionData.prev_time).first()
        if motion is not None:
            print(motion.prev_state.zone.name)
            print(motion.prev_state.time)
            print(motion.next_state.time)
            print(motion.prev_state.zone.config)
            motion.calculate_motion_ds(processor)
            motion_calculated = True
    



