
from meteo import MeteoZone
from meteo_raw import MeteoRawData

import sql
import urllib
import urllib.request
import re
from datetime import datetime
import dateutil.parser
import numpy as np
from PIL import Image

from sqlalchemy import exists
import io

class RawImageGrabber(object):
    def __init__(self, zone, index_url_schema, file_url_schema, data_filter=lambda _: True):
        self.zone = zone
        self.index_url_schema = index_url_schema
        self.file_url_schema = file_url_schema
        self.data_filter = data_filter


    def _get_image(self, raw_data, filename):
        print('Grabbing', filename)
        url = self.file_url_schema % {
                    'channel': raw_data.channel,
                    'satellite': raw_data.satellite,
                    'zone': raw_data.zone,
                    'filename': filename
                }
        with urllib.request.urlopen(url) as response:
            image = np.array(Image.open(io.BytesIO(response.read())), np.float32, copy=True, order='F')/255.0
        return image

    def get_raw_datas(self):
        raw_datas = []
        for satellite in self.zone.config['satellites']:
            for channel in self.zone.config['channels']:
                print(self.zone.name)
                print(channel)
                print(satellite)
                url = self.index_url_schema % {
                            'channel': channel,
                            'satellite': satellite,
                            'zone': self.zone.name
                        }
                print(url)
                with urllib.request.urlopen(url) as response:
                   html = response.read()
                   matches = set(re.findall('[0-9GI]{16}\.tif', str(html, 'ascii')))

                raw_datas += [(match, MeteoRawData(
                                datetime.strptime(match[:10], "%y%m%d%H%M"),
                                self.zone.name,
                                satellite,
                                channel))
                                    for match in set(matches)]
        start_date = dateutil.parser.parse(self.zone.config['start_date'])
        raw_datas = [(match, data) for match, data in raw_datas
                        if data.time >= start_date and self.data_filter(data)]
        for match, data in raw_datas:
            print(match, data.__dict__)
        for match, data in raw_datas:
            data.image = self._get_image(data, match)

        return [data for match, data in raw_datas]



index_url_schema = 'http://goes.gsfc.nasa.gov/%(satellite)s/%(zone)s/%(channel)s/'
file_url_schema = 'http://goes.gsfc.nasa.gov/%(satellite)s/%(zone)s/%(channel)s/%(filename)s'


with sql.session_scope() as session:
    def existing_data_filter(raw_data):
        (retval, ), = session.query(exists() \
            .where(MeteoRawData.channel==raw_data.channel) \
            .where(MeteoRawData.satellite==raw_data.satellite) \
            .where(MeteoRawData.time==raw_data.time) \
            .where(MeteoRawData.zone==raw_data.zone)
            )
        return not retval
    grabbers = [RawImageGrabber(zone, index_url_schema, file_url_schema, existing_data_filter)
                    for zone in session.query(MeteoZone).all()]
    # Grab all datas (metadata) from web
    datas = [data
                for grabber in grabbers
                    for data in grabber.get_raw_datas()]
    for data in datas:
        session.add(data)
    session.commit()

    print(datas)

