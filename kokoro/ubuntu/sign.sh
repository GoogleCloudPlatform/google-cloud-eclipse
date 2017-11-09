#!/bin/bash

# Fail on any error.
set -e
# Display commands being run.
set -x

echo ${KOKORO_GFILE_DIR}
cd $KOKORO_GFILE_DIR

mkdir -p signed && chmod 777 signed
mkdir -p signed/plugins && chmod 777 signed/plugins
mkdir -p signed/features && chmod 777 signed/features
 
cp index.html metadata.product metadata.p2.inf signed/

FILES=plugins/*.jar
for f in $FILES
do
  echo "Processing $f file..."
  filename=$(basename "$f")
  if /escalated_sign/escalated_sign.py -j /escalated_sign_jobs -t signjar \
    "$KOKORO_GFILE_DIR/plugins/$filename" \
    "$KOKORO_GFILE_DIR/signed/plugins/$filename"
  then echo "Signed $filename"
  else 
    cp "$KOKORO_GFILE_DIR/plugins/$filename" "$KOKORO_GFILE_DIR/signed/plugins/$filename"
  fi
done

FEATURES=features/*.jar
for f in $FEATURES
do
  echo "Processing $f file..."
  filename=$(basename "$f")
  /escalated_sign/escalated_sign.py -j /escalated_sign_jobs -t signjar \
    "$KOKORO_GFILE_DIR/features/$filename" \
    "$KOKORO_GFILE_DIR/signed/features/$filename"
  echo "Signed $filename"
done
