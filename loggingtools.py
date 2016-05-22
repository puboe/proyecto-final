import logging
import time
from contextlib import contextmanager

def create_timefunction(logger):
    def timefunction(function, fname=None, params=False):
        fname = fname or str(function)
        def wrapper(*args, **kwargs):
            if params:
                logger.debug('Enter %s (%s, %s)' % (fname, str(args), str(kwargs)))
            else:
                logger.debug('Enter %s' % (fname,))
            start_time = time.time()
            result = function(*args, **kwargs)
            elapsed_time = time.time() - start_time
            logger.debug('Exit %s (%f sec)' % (fname, elapsed_time))
            return result
        return wrapper
    return timefunction

def create_logblock(logger):
    @contextmanager
    def logblock(bname):
        logger.debug('Enter %s' % (bname,))
        start_time = time.time()
        yield
        elapsed_time = time.time() - start_time
        logger.debug('Exit %s (%f sec)' % (bname, elapsed_time))
    return logblock
