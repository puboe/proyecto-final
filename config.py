class Config(object):
    pass

class DevelopmentConfig(Config):
    SQLALCHEMY_DATABASE_URI = 'postgresql+psycopg2://piedpiper:piedpiper@gserver/piedpiper'
    DEBUG = True

class DevelopmentExternalConfig(Config):
    SQLALCHEMY_DATABASE_URI = 'postgresql+psycopg2://piedpiper:piedpiper@localhost/piedpiper'
    DEBUG = True

class ProductionConfig(Config):
    DEBUG = False
    SQLALCHEMY_DATABASE_URI = 'postgresql+psycopg2://piedpiper:piedpiper@localhost/piedpiper'

