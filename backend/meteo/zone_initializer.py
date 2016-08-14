import argparse
from meteo.zone_config import create_zone
from meteo.updater import update
from meteo.motion_calculator import calculate_all, create_all_missing_motions
from meteo.background_extractor import extract_all_backgrounds
from meteo.connector import create_schema

parser = argparse.ArgumentParser(description='Daemonize meteo modules')
parser.add_argument('zone_file', help = 'Zone file to create. check meteo/resources/zones')
args = parser.parse_args()

print('Creating schema...')
create_schema()
print('Creating zone...')
create_zone(args.zone_file)
print('Updating zone images...')
update()
print('Extracting backgrounds...')
extract_all_backgrounds()
print('Calculating motions...')
create_all_missing_motions()
calculate_all()




