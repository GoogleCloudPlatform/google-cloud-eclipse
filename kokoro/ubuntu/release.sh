#!/bin/bash

# Fail on any error.
set -e
# Display commands being run.
set -x

# create a timestamp file
date +%s > timestamp.txt

git/google-cloud-eclipse/kokoro/ubuntu/sign.sh


