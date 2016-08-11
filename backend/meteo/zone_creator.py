import json
import sys
from .meteo_sql import MeteoZone
from .connector import session_scope
from .image import ImageProcessor
from PIL import Image
import numpy as np

processor = ImageProcessor()

if len(sys.argv) < 2:
    print("Usage: %s [zone_file.json]" % sys.argv[0])
    exit(1)

zone_file = sys.argv[1]
with open(zone_file, 'r') as f:
    zone_attr = json.load(f)

def load_cropped(image_file, crop_rect):
    return processor.crop(np.array(Image.open(image_file), np.float32, copy=True, order='F')/255.0, crop_rect)

map_image = load_cropped(
                zone_attr['map_image'],
                zone_attr['config']['crop_rect'])

zone = MeteoZone(name=zone_attr['name'],
                 map_image=map_image,
                 config=zone_attr['config'])

with session_scope() as session:
    session.merge(zone)

