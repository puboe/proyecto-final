#! /usr/bin/env python3

import argparse
import daemon
import lockfile
import tempfile
import time
import os
import importlib

parser = argparse.ArgumentParser(description='Daemonize meteo modules')
parser.add_argument('module', help = 'Module to daemonize. Must implement Daemon class.')
parser.add_argument('action', choices=['start', 'stop', 'restart'], help='Commands may be: "start", "stop" or "restart". If the argument is not specified, the program will be run out of daemon mode')

args = parser.parse_args()


pid_file_name = args.module + '.pid'
pid_file_path = os.path.join(tempfile.gettempdir(), pid_file_name)
stdout_file_name = args.module + '.out'
stdout_file_path = os.path.join(tempfile.gettempdir(), stdout_file_name)
stderr_file_name = args.module + '.err'
stderr_file_path = os.path.join(tempfile.gettempdir(), stderr_file_name)
module = importlib.import_module(args.module)
context = module.Daemon(
                    stdout=stdout_file_path,
                    stderr=stderr_file_path,
                    pidfile=pid_file_path)


if args.action == 'start':
    context.start()
elif args.action == 'stop':
    context.stop()
elif args.action == 'restart':
    context.restart()

