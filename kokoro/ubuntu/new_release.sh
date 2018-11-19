#!/bin/bash
#
# Builds CT4E with Maven.
#

# Fail on any error.
set -o errexit
# Display commands being run.
set -o xtrace

gsutil -q cp "gs://ct4e-m2-repositories-for-kokoro/m2-cache.tar" - \
  | tar -C "${HOME}" -xf -

export CLOUDSDK_CORE_DISABLE_USAGE_REPORTING=true
# Workaround for https://issuetracker.google.com/119096137, until gcloud in
# the Kokoro base image gets updated to make the issue obsolete.
gcloud components remove container-builder-local

gcloud components update --quiet
gcloud components install app-engine-java --quiet

echo "OAUTH_CLIENT_ID: ${OAUTH_CLIENT_ID}"
echo "OAUTH_CLIENT_SECRET: ${OAUTH_CLIENT_SECRET}"
echo "ANALYTICS_TRACKING_ID: ${ANALYTICS_TRACKING_ID}"
echo "PRODUCT_VERSION_SUFFIX: "${PRODUCT_VERSION_SUFFIX}

# Exit if undefined (zero-length).
test -n "${OAUTH_CLIENT_ID}"
test -n "${OAUTH_CLIENT_SECRET}"
test -n "${ANALYTICS_TRACKING_ID}"

cd git/google-cloud-eclipse

# A few notes on the Maven command:
#    - Need to unset `TMPDIR` for `xvfb-run` due to a bug:
#      https://bugs.launchpad.net/ubuntu/+source/xorg-server/+bug/972324
#    - Single-quotes are necessary for `-Dproduct.version.qualifier.suffix`,
#      since it should be appended as a constant string in a date format.
TMPDIR= xvfb-run \
  mvn -V -B \
      -Doauth.client.id="${OAUTH_CLIENT_ID}" \
      -Doauth.client.secret="${OAUTH_CLIENT_SECRET}" \
      -Dga.tracking.id="${ANALYTICS_TRACKING_ID}" \
      ${PRODUCT_VERSION_SUFFIX:+-Dproduct.version.qualifier.suffix="'${PRODUCT_VERSION_SUFFIX}'"} \
    clean verify
