#!/bin/bash

# Fail on any error.
set -e
# Display commands being run.
set -x

CLOUDSDK_CORE_DISABLE_USAGE_REPORTING=true
DISPLAY=:99.0

sh -e /etc/init.d/xvfb start
sleep 3 # give xvfb some time to start
metacity --sm-disable --replace &
sleep 3 

cd github/google-cloud-eclipse
mvn -Ptravis --fail-at-end verify
