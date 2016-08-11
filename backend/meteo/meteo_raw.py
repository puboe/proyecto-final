

class MeteoRawData(object):
    def __init__(self, time, zone, satellite, channel, image=None):
        self.time = time
        self.zone = zone
        self.satellite = satellite
        self.channel = channel
        self.image = image



