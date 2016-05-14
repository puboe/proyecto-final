import os
from ftplib import FTP
import time

FTP_SERVER = "goes.gsfc.nasa.gov"
FTP_DIR = "/pub/goeseast/argentina/ir2/"
IMG_DIR = "images/"
FILEMATCH = "*.tif"
SCHEDULED_TIME = 30 * 60
MIN_SIZE = 100 * 1024 # Min size in bytes

def fetch():
	try:
		ftp = FTP(FTP_SERVER)
	except:
		print "Couldn't find server"
		return
	
	ftp.login()
	ftp.cwd(FTP_DIR)

	skipped = 0
	downloaded = 0

	for filename in ftp.nlst(FILEMATCH):
		if os.path.exists(IMG_DIR + filename) or ftp.size(filename) < MIN_SIZE:
			skipped += 1
		else:
			fhandle = open(os.path.join(IMG_DIR, filename), 'wb')
			print 'Getting ' + filename
			ftp.retrbinary('RETR ' + filename, fhandle.write)
			fhandle.close()
			downloaded += 1

	print str(downloaded) + " files downloaded, " + str(skipped) + " files skipped."
			
	ftp.close()

def daemon():
	while True:
		fetch()
		time.sleep(SCHEDULED_TIME)

if __name__ == "__main__":
	daemon()
