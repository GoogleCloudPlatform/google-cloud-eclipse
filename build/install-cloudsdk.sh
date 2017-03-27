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
set -o errexit -o nounset

CLOUDSDK_VERSION=$1
INSTALLDIR=$2

CLOUDSDKDIR=${INSTALLDIR}/google-cloud-sdk

currentVersion=$(if [ -f ${CLOUDSDKDIR}/VERSION ]; then cat ${CLOUDSDKDIR}/VERSION; fi)

if [ "${currentVersion}" = "${CLOUDSDK_VERSION}" ]; then
    echo "Google Cloud SDK at version $(cat ${CLOUDSDKDIR}/VERSION)"
    exit 0
elif [ -n "${currentVersion}" ]; then
    echo "Replacing found Google Cloud SDK version ${currentVersion}"
fi

# Remove the install if we are killed
# XXX can this be arranged to happen with `set -o errexit`?
die() {
    if [ $# -gt 0 ]; then
        echo "$0: $*" 1>&2
    else
        echo "$0: ERROR" 1>&2
    fi
    rm -rf ${CLOUDSDKDIR}
    exit 1
}

rm -rf ${CLOUDSDKDIR}

echo ">> download google-cloud-sdk-${CLOUDSDK_VERSION}"
wget https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${CLOUDSDK_VERSION}-linux-x86_64.tar.gz || die
tar -xzf google-cloud-sdk-${CLOUDSDK_VERSION}-linux-x86_64.tar.gz -C ${INSTALLDIR} || die

## DISABLED: Updating to latest version should be done outside
## update all Cloud SDK components
# echo ">> gcloud components update"
# ${CLOUDSDKDIR}/bin/gcloud components update --quiet || die

# add App Engine component to Cloud SDK
echo ">> gcloud components install app-engine-java"
${CLOUDSDKDIR}/bin/gcloud components install app-engine-java --quiet || die


