import os
from ftplib import FTP
import time
import logging

FTP_SERVER = "goes.gsfc.nasa.gov"
FTP_DIR = "/pub/goeseast/argentina/ir2/"
IMG_DIR = "images/"
FILEMATCH = "*.tif"
SCHEDULED_TIME = 30 * 60
MIN_SIZE = 100 * 1024 # Min size in bytes
LOG_FILE = "log/ftp-service.log"

def fetch():
	try:
		logging.info("Connecting to server %s", FTP_SERVER)
		ftp = FTP(FTP_SERVER)
	except:
		logging.error("Couldn't find server %s", FTP_SERVER)
		return
	
	ftp.login()
	ftp.cwd(FTP_DIR)

	skipped = 0
	downloaded = 0

	for filename in ftp.nlst(FILEMATCH):
		if filename == "latest.tif" or os.path.exists(IMG_DIR + filename) or ftp.size(filename) < MIN_SIZE:
			skipped += 1
		else:
			fhandle = open(os.path.join(IMG_DIR, filename), 'wb')
			logging.info('Getting %s', filename)
			ftp.retrbinary('RETR ' + filename, fhandle.write)
			fhandle.close()
			downloaded += 1

	logging.info('%s files downloaded, %s files skipped.', downloaded, skipped)
			
	ftp.close()

def config_logging():
	logging.basicConfig(filename=LOG_FILE, level=logging.INFO, 
		format='%(asctime)s %(levelname)s: %(message)s', datefmt='%d/%m/%Y %H:%M:%S')

def daemon():
	config_logging()
	while True:
		fetch()
		time.sleep(SCHEDULED_TIME)

if __name__ == "__main__":
	daemon()
