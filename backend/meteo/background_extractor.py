from .connector import session_scope
from .meteo_sql import *
from . import image
import pyopencl
import sys

import datetime
import numpy as np



def extract_all_backgrounds():
    with session_scope() as session:
        for zone, channel, satellite in session \
                .query(MeteoZone, MeteoStaticData.channel, MeteoStaticData.satellite) \
                .distinct():
            print(zone.name, satellite, channel)
            background = session.query(MeteoBackgroundData).filter_by(zone_name=zone.name,
                                                                      satellite=satellite,
                                                                      channel=channel).first()
            background = background or MeteoBackgroundData(zone=zone,
                                                           satellite=satellite,
                                                           channel=channel)

            data_query = session.query(MeteoStaticData) \
                                .filter_by(zone_name=zone.name, is_valid=True) \
                                .filter(MeteoStaticData.channel == channel) \
                                .filter(MeteoStaticData.satellite == satellite)

            data_count = data_query.count()
            background_image = np.copy(data_query.first().image)
            for data in data_query.all():
                background_image[background_image == 0.0] = data.image[background_image == 0.0]
                background_image[data.image > 0.0] = np.min([background_image[data.image > 0.0], data.image[data.image > 0.0]], axis=0)
                #background_image = background_image + data.image/data_count
            background.image = background_image
            session.commit()

        #session.rollback()


if __name__ == '__main__':
    extract_all_backgrounds()
