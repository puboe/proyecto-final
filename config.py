import importlib
import os

class Config(object):
    pass

class DevelopmentConfig(Config):
    SQLALCHEMY_DATABASE_URI = 'postgresql+psycopg2://piedpiper:piedpiper@gserver/piedpiper'
    DEBUG = True

class DevelopmentExternalConfig(Config):
    SQLALCHEMY_DATABASE_URI = 'postgresql+psycopg2://piedpiper:piedpiper@localhost/piedpiper'
    DEBUG = True

class DevelopmentLocalConfig(Config):
    SQLALCHEMY_DATABASE_URI = 'postgresql+psycopg2://piedpiper:piedpiper@exp/piedpiper'
    DEBUG = True

class ProductionConfig(Config):
    DEBUG = False
    SQLALCHEMY_DATABASE_URI = 'postgresql+psycopg2://piedpiper:piedpiper@localhost/piedpiper'


def load_class(class_path):
    class_path_fields = class_path.split(".")
    module_path = ".".join(class_path_fields[:-1])
    class_name = class_path_fields[-1]
    module = importlib.import_module(module_path)
    return getattr(module, class_name)

EnvironmentConfig = load_class(os.environ['APP_SETTINGS']) if os.environ.get('APP_SETTINGS', None) else None
