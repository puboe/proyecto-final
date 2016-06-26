from flask import request, send_file
import dateutil.parser
from werkzeug.routing import BaseConverter
import numpy as np
import io
from PIL import Image

class DateTimeConverter(BaseConverter):
    def to_python(self, value):
        return dateutil.parser.parse(value)

    def to_url(self, value):
        return value.isoformat()

def render_image_array(array):
    image = Image.fromarray((array*255.0).astype(np.uint8))
    output = io.BytesIO()
    image.save(output, format='PNG')
    return send_file(io.BytesIO(output.getvalue()), mimetype='image/png')

def request_wants_json():
    best = request.accept_mimetypes \
        .best_match(['application/json', 'text/html'])
    return best == 'application/json' and \
        request.accept_mimetypes[best] > \
        request.accept_mimetypes['text/html']
