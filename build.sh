#!/bin/bash

# Fail on any error.
set -e
# Display commands being run.
set -x

if [ "$1" == "release" ]; then
  # TODO load keys from internal storage
  mvn clean install
else
  mvn clean install
fi
