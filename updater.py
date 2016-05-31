from meteo_sql import *
from connector import *

import urllib
import urllib.request
import urllib.parse
import re
from datetime import datetime
import dateutil.parser
import numpy as np
from PIL import Image

from sqlalchemy import exists
import io

from image import ImageProcessor


class GOESScrapper(object):
    def __init__(self, satellite, zone_name, channel, url_schema=None, match_regex=None, time_parser=None):
        self.satellite = satellite
        self.zone_name = zone_name
        self.channel = channel
        self.url_schema = url_schema or 'http://goes.gsfc.nasa.gov/%(satellite)s/%(zone)s/%(channel)s/'
        self.match_regex = match_regex or '[0-9GI]{16}\.tif'
        self.time_parser = time_parser or (lambda fn: datetime.strptime(fn[:10], "%y%m%d%H%M"))
        self.reset()

    def reset(self):
        self.results = None

    def get(self):
        if self.results is None:
            self.scrap()

        return self.results

    def scrap(self):
        url = self.url_schema % {
                    'satellite': self.satellite,
                    'zone': self.zone_name,
                    'channel': self.channel
                }
        with urllib.request.urlopen(url) as response:
           html = response.read()
           matches = set(re.findall(self.match_regex, str(html, 'ascii')))

        self.results = [{'time': self.time_parser(match),
                         'satellite': self.satellite,
                         'zone_name': self.zone_name,
                         'channel': self.channel,
                         'image': urllib.parse.urljoin(url, match)}
                            for match in matches]

class MeteoZoneUpdater(object):
    def __init__(self, session, zone):
        self.zone = zone
        self.session = session
        sources = [source.split('/') for source in zone.config['sources']]
        self.scrappers = [GOESScrapper(satellite, zone.name, channel) 
                            for satellite, channel in sources]
        self.start_date = dateutil.parser.parse(self.zone.config['start_date'])

    def reset(self):
        for scrapper in self.scrappers:
            scrappers.reset()

    def update_states(self):
        available = set([data['time']
                            for scrapper in self.scrappers
                                for data in scrapper.get()
                                    if data['time'] >= self.start_date])


        states = [MeteoState(time=time, zone_name=self.zone.name)
                    for time in available]

        for state in states:
            session.merge(state)

        return states


    def _data_is_new(self, data):
        query = exists()
        for attr in ['time', 'zone_name', 'satellite', 'channel']:
            query = query.where(getattr(MeteoStaticData, attr)==data[attr])
        return not session.query(query).scalar()

    def _fetch_and_crop(self, data):
        data = dict(data)
        url = data['image']
        with urllib.request.urlopen(url) as response:
            image = np.array(Image.open(io.BytesIO(response.read())), np.float32, copy=True, order='F')/255.0

        processor = ImageProcessor()
        image = processor.crop(image, self.zone.config['crop_rect']) 
        data['image'] = None if processor.is_black(image) else image
        return data

    def update_datas(self):
        available = [data for scrapper in self.scrappers
                            for data in scrapper.get()
                                if data['time'] >= self.start_date]

        datas = filter(self._data_is_new, available)
        datas = map(self._fetch_and_crop, datas)

        for data in datas:
            print(data['time'].isoformat(), data['satellite'], data['channel'])
            session.add(MeteoStaticData(**data))


with session_scope() as session:
    updaters = [MeteoZoneUpdater(session, zone)
                    for zone in session.query(MeteoZone).all()]
    print('Updating zones:', [updater.zone.name for updater in updaters])
    for updater in updaters:
        print('Updating', updater.zone.name)
        states = updater.update_states()
        states.sort(key=lambda s: s.time)
        print('\tScrapping data')
        print('\tFound states: ')
        for state in states:
            print ('\t', state.time.isoformat())
        print('\tDownloading new images')
        updater.update_datas()
        print('Done')

