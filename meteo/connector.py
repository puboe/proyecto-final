import config
_CONNECT_URI = config.EnvironmentConfig.SQLALCHEMY_DATABASE_URI
_ENGINE = None
_SESSION_MAKER = None

from contextlib import contextmanager
from sqlalchemy.orm import sessionmaker
from sqlalchemy import create_engine

def get_engine(*args, connect_uri=_CONNECT_URI, **kwargs):
    global _ENGINE
    if _ENGINE is None:
        _ENGINE = create_engine(connect_uri)
    return _ENGINE

def get_session_maker(*args, **kwargs):
    global _SESSION_MAKER
    if _SESSION_MAKER is None:
        _SESSION_MAKER = sessionmaker(*args, bind=get_engine(*args, **kwargs), **kwargs)
    return _SESSION_MAKER

def get_session(*args, **kwargs):
    return get_session_maker(*args, **kwargs)()

@contextmanager
def session_scope():
    """Provide a transactional scope around a series of operations."""
    session = get_session()
    try:
        yield session
        session.commit()
    except:
        session.rollback()
        raise
    finally:
        session.close()

if __name__ == '__main__':
    import code
    from .meteo_sql import *
    Base.metadata.create_all(get_engine())
    Session = get_session_maker()
    session = Session()
    code.interact(local=locals())
