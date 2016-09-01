#!/bin/bash

# Fail on any error.
set -e
# Display commands being run.
set -x

cd git/kokoro-codelab-elharo
./build.sh


