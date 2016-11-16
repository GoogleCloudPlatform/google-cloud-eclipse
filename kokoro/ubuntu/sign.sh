#!/bin/bash

# Fail on any error.
set -e
# Display commands being run.
set -x

echo ${KOKORO_GFILE_DIR}
cd $KOKORO_GFILE_DIR
mkdir -p signed && chmod 777 signed
mkdir -p signed/plugins && chmod 777 signed/plugins
 
FILES=$KOKORO_GFILE_DIR/plugins/*.jar
for f in $FILES
do
  echo "Processing $f file..."
  # signed=$(echo $f | sed 's/\/\(.*\)\/\(.*\)\.jar/\/\1\/signed\/\2.jar/g')
  filename=$(echo $f | sed 's/\/\(.*\)\/\(.*\)\.jar/\2.jar/g')
  echo "Signing $filename"
  if /escalated_sign/escalated_sign.py -j /escalated_sign_jobs -t signjar \
    $KOKORO_GFILE_DIR/plugins/$filename \
    $KOKORO_GFILE_DIR/signed/plugins/$filename
  then echo "Signed $filename"
  # else cp the already signed jar
  fi
done