#!/bin/sh
# Download and install the Google Cloud SDK

if [ $# -ne 2 ]; then
    echo "Ensure Google Cloud SDK in <install-dir>/google-cloud-sdk is"
    echo "at desired version"
    echo "use: $0 <cloudsdk-version> <install-dir>"
    echo "ex: $0 147.0.0 /tmp"
    exit 1
fi

# ensure we error out on failures and unset variables
set -o pipefail -o errexit -o nounset

CLOUDSDK_VERSION=$1
INSTALLDIR=$2

CLOUDSDKDIR=${INSTALLDIR}/google-cloud-sdk

getCurrentVersion() {
    if [ -d ${CLOUDSDKDIR} -a -f ${CLOUDSDKDIR}/VERSION ]; then
        cat ${CLOUDSDKDIR}/VERSION
    fi
}

if [ "$(getCurrentVersion)" = "${CLOUDSDK_VERSION}" ]; then
    echo "Google Cloud SDK at version $(cat ${CLOUDSDKDIR}/VERSION)"
    exit 0
fi

# Remove the install if we are killed
trap "rm -rf ${CLOUDSDKDIR}" 1 2 15
rm -rf ${CLOUDSDKDIR}
wget https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${CLOUDSDK_VERSION}-linux-x86_64.tar.gz
tar -xzf google-cloud-sdk-${CLOUDSDK_VERSION}-linux-x86_64.tar.gz -C ${INSTALLDIR}

## DISABLED: Updating to latest version should be done outside
## update all Cloud SDK components
# ${CLOUDSDKDIR}/bin/gcloud components update --quiet

# add App Engine component to Cloud SDK
${CLOUDSDKDIR}/bin/gcloud components install app-engine-java --quiet


