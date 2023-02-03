#!/bin/bash
#
# Builds CT4E with Maven.
#

# Fail on any error.
set -o errexit


gsutil -q cp "gs://ct4e-m2-repositories-for-kokoro/m2-cache.tar" - \
  | tar -C "${HOME}" -xf -

export CLOUDSDK_CORE_DISABLE_USAGE_REPORTING=true
gcloud components update --quiet

# More recent Cloud SDK requires Python 3.5 (b/194714889)
if [ -z "$CLOUDSDK_PYTHON" ]; then
    export CLOUDSDK_PYTHON=python3.5
fi
gcloud components install app-engine-java --quiet


# Exit if undefined (zero-length).
[[ -n "${OAUTH_CLIENT_ID}" ]]
[[ -n "${OAUTH_CLIENT_SECRET}" ]]
[[ -n "${FIRELOG_API_KEY}" ]]

sudo update-java-alternatives --set java-1.11.0-openjdk-amd64
export JAVA_HOME=/usr/lib/jvm/java-1.11.0-openjdk-amd64

cd github/google-cloud-eclipse

sed -i.bak -e "s,JDK_8_HOME,/usr/lib/jvm/java-1.8.0-openjdk-amd64," .github/workflows/toolchains.xml
sed -i.bak -e "s,JDK_11_HOME,/usr/lib/jvm/java-1.11.0-openjdk-amd64," .github/workflows/toolchains.xml

# A few notes on the Maven command:
#    - Need to unset `TMPDIR` for `xvfb-run` due to a bug:
#      https://bugs.launchpad.net/ubuntu/+source/xorg-server/+bug/972324
#    - Single-quotes are necessary for `-Dproduct.version.qualifier.suffix`,
#      since it should be appended as a constant string in a date format.
TMPDIR= xvfb-run \
  mvn -V -B \
      --toolchains=.github/workflows/toolchains.xml \
      -DskipTests=true \
      -Doauth.client.id="${OAUTH_CLIENT_ID}" \
      -Doauth.client.secret="${OAUTH_CLIENT_SECRET}" \
      -Dfirelog.api.key="${FIRELOG_API_KEY}" \
      ${PRODUCT_VERSION_SUFFIX:+-Dproduct.version.qualifier.suffix="'${PRODUCT_VERSION_SUFFIX}'"} \
    clean install
