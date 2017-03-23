#!/bin/bash

# Fail on any error.
set -e
# Display commands being run.
set -x

cd github/google-cloud-eclipse
mvn -Ptravis --fail-at-end verify
