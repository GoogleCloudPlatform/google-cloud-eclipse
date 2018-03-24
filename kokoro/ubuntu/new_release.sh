#!/bin/bash
#
# Builds CT4E with Maven.
#

# Fail on any error.
set -e
# Display commands being run.
set -x

export CLOUDSDK_CORE_DISABLE_USAGE_REPORTING=true
gcloud components update --quiet
gcloud components install app-engine-java --quiet

echo "OAUTH_CLIENT_ID: ${OAUTH_CLIENT_ID}"
echo "OAUTH_CLIENT_SECRET: ${OAUTH_CLIENT_SECRET}"
echo "ANALYTICS_TRACKING_ID: ${ANALYTICS_TRACKING_ID}"
echo "CT4E_DISPLAY_VERSION: ${CT4E_DISPLAY_VERSION}"

# Exit if undefined (zero-length).
test -n "${OAUTH_CLIENT_ID}"
test -n "${OAUTH_CLIENT_SECRET}"
test -n "${ANALYTICS_TRACKING_ID}"
test -n "${CT4E_DISPLAY_VERSION}"

cd git/google-cloud-eclipse

# Drop the `.qualifier` in `metadata.product` and use the exact release
# version. (For example, `version="1.3.0.qualifier"` to `version="1.3.0"`.)
# version.
#
# https://github.com/GoogleCloudPlatform/google-cloud-eclipse/pull/2363#issuecomment-327844378
# https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2211
xmlstarlet ed --inplace -u '/product/@version' -v "${CT4E_DISPLAY_VERSION}" \
  gcp-repo/metadata.product

# Need to unset `TMPDIR` for `xvfb-run` due to a bug:
# https://bugs.launchpad.net/ubuntu/+source/xorg-server/+bug/972324
TMPDIR= xvfb-run \
  mvn -V -B -Doauth.client.id="${OAUTH_CLIENT_ID}" \
            -Doauth.client.secret="${OAUTH_CLIENT_SECRET}" \
            -Dga.tracking.id="${ANALYTICS_TRACKING_ID}" \
    clean verify

# Also export `metadata.product` and `metadata.p2.inf` to the second Kokoro job.
readonly METADATA_DIR=gcp-repo/target/repository/metadata
mkdir "${METADATA_DIR}"
cp gcp-repo/metadata.p2.inf gcp-repo/metadata.product "${METADATA_DIR}"
