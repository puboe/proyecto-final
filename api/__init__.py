import os
from flask import Flask, Blueprint
from flask_sqlalchemy import SQLAlchemy
from api.util import DateTimeConverter, DateTimeJSONEncoder

app = Flask(__name__)
app.config.from_object(os.environ['APP_SETTINGS'])
app.url_map.converters['datetime'] = DateTimeConverter
app.json_encoder = DateTimeJSONEncoder
db = SQLAlchemy(app, session_options={'autocommit': False})
blueprint = Blueprint('blueprint', __name__,
                      template_folder='templates',
                      static_folder='static',
                      url_prefix=app.config['APPLICATION_ROOT'])

import api.views.zone
import api.views.state
import api.views.static_data
import api.views.prediction_data
import api.views.flow
import api.views.fixed_flow

from flask import redirect, url_for
from meteo.meteo_sql import MeteoZone


@blueprint.route('/test')
def test():
    return 'Test'

@blueprint.route('/')
def root():
    first_zone = db.session.query(MeteoZone).first()
    if first_zone is None:
        abort(404)
    return redirect(url_for('blueprint.show_zone', zone_name=first_zone.name))

app.register_blueprint(blueprint)
