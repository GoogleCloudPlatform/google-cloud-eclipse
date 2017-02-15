#!/bin/bash

# Fail on any error.
set -e
# Display commands being run.
set -x

echo ${KOKORO_GFILE_DIR}
cd $KOKORO_GFILE_DIR
mkdir -p signed && chmod 777 signed
/escalated_sign/escalated_sign.py -j /escalated_sign_jobs -t signjar \
 $KOKORO_GFILE_DIR/artifacts.jar \
 $KOKORO_GFILE_DIR/signed/artifacts.jar
/escalated_sign/escalated_sign.py -j /escalated_sign_jobs -t signjar \
 $KOKORO_GFILE_DIR/content.jar \
 $KOKORO_GFILE_DIR/signed/content.jar