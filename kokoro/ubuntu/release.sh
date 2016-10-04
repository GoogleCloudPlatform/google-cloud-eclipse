#!/bin/bash

# Fail on any error.
set -e
# Display commands being run.
set -x

# create a timestamp file
date +%s > timestamp.txt

cd git/google-cloud-eclipse
./sign.sh


