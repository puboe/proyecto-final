import os
from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from api.util import DateTimeConverter, DateTimeJSONEncoder

app = Flask(__name__)
app.config.from_object(os.environ['APP_SETTINGS'])
app.url_map.converters['datetime'] = DateTimeConverter
app.json_encoder = DateTimeJSONEncoder
db = SQLAlchemy(app, session_options={'autocommit': False})

import api.views.zone
import api.views.state
import api.views.static_data
import api.views.prediction_data
import api.views.flow

from flask import redirect, url_for
from meteo.meteo_sql import MeteoZone


@app.route('/test')
def test():
    return 'Test'

@app.route('/')
def root():
    first_zone = db.session.query(MeteoZone).first()
    if first_zone is None:
        abort(404)
    return redirect(url_for('show_zone', zone_name=first_zone.name))
