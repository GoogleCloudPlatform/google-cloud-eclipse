#!/bin/sh

# This script starts with Step 15 in the release process (i.e., after Kokoro
# has built and signed the binaries.)
#
# It is safe to rerun this script again when something goes wrong, provided
# that you didn't complete the final step, which means you are done. (The final
# step grants public access to the files uploaded to cloud-tools-for-eclipse.)
#
# For debugging: this script creates a temp work directory to download and
# generate various files (typically under "/tmp" on Linux). The directory can
# be inspected later if anything goes wrong.

die() {
    for line in "$@"; do
        echo $line
    done
    exit 1
}

ask_proceed() {
    while true; do
        echo -n "Proceed ([Y]/n)? "
        read ANSWER
        [ "$ANSWER" == 'n' -o "$ANSWER" == 'N' ] && exit 1
        [ "$ANSWER" == 'y' -o "$ANSWER" == 'Y' -o -z "$ANSWER" ] && break
        echo -n "Invalid answer. "
    done
}

[ -z "$ECLIPSE_HOME" ] && \
    die 'The release steps require running an Eclipse binary.' \
        'Set $ECLIPSE_HOME before running this script.'
[ ! -x "$ECLIPSE_HOME/eclipse" ] && \
    die "'$ECLIPSE_HOME/eclipse' is not an executable. Halting."

if ! $(command -v xmllint >/dev/null); then
    die "Cannot find xmllint. Halting."
fi

WORK_DIR=$( mktemp -d )
SIGNED_DIR=$WORK_DIR/signed
LOCAL_REPO=$WORK_DIR/repository

echo "#"
echo "# Using '$WORK_DIR' as a temp work directory."
echo "#"

###############################################################################
echo
echo "#"
echo "# Copy files built (signed) by Kokoro"
echo "# into '$SIGNED_DIR'."
echo "#"

while true; do
    echo -n "Enter GCS bucket URL taken from the Kokoro page: "
    read GCS_URL
    GCS_BUCKET_PATH=$( echo $GCS_URL | \
        grep -o 'signedjars/staging/prod/google-cloud-eclipse/ubuntu/release/[0-9]\+/[0-9-]\+' )
    if [ $? -eq 0 ]; then
        break
    fi
    echo "Could not extract GCS bucket path from the URL."
done

set -x
mkdir $SIGNED_DIR && \
    gsutil -m cp -R gs://$GCS_BUCKET_PATH/gfile/signed/* $SIGNED_DIR
set +x

if [ 0 -eq $( ls -1 $SIGNED_DIR | wc -l ) ]; then
    die "'$SIGNED_DIR' is empty. Wrong GCS bucket URL provided?" \
        "Fix the problem and rerun the script. Halting."
fi

echo "#"
echo "# Now verifying whether jars are all signed."
echo "#"
for jar in $SIGNED_DIR/plugins/com.google.cloud.tools.eclipse.*.jar \
        $SIGNED_DIR/features/com.google.cloud.tools.eclipse.*.jar; do
    echo -n "$( basename $jar ): "
    if ! jarsigner -strict -verify $jar; then
        die "Unsigned artifact found. Halting."
    fi
done

###############################################################################
echo
echo "#"
echo "# Verify if constants have been injected..."
echo "#"

LOGIN_CONSTANTS=$( javap -private -classpath \
    $SIGNED_DIR/plugins/com.google.cloud.tools.eclipse.login_*.jar \
    -constants com.google.cloud.tools.eclipse.login.Constants | \
    grep OAUTH_CLIENT_ )
ANALYTICS_CONSTANT=$( javap -classpath \
    $SIGNED_DIR/plugins/com.google.cloud.tools.eclipse.usagetracker_*.jar \
    -constants com.google.cloud.tools.eclipse.usagetracker.Constants | \
    grep ANALYTICS_TRACKING_ID )
echo -e "$LOGIN_CONSTANTS\n$ANALYTICS_CONSTANT"

# Verify if the constants have been injected.
if echo "${LOGIN_CONSTANTS}${ANALYTICS_CONSTANT}" | grep -q @; then
    die "Some constant(s) have not been injected. Halting."
fi

# Verify if the injected constants have the right lengths.
OAUTH_CLIENT_ID=$( echo "$LOGIN_CONSTANTS" | grep "OAUTH_CLIENT_ID" | \
    sed -e 's/.* = "\(.*\)"; */\1/' )
OAUTH_CLIENT_SECRET=$( echo "$LOGIN_CONSTANTS" | grep "OAUTH_CLIENT_SECRET" | \
    sed -e 's/.* = "\(.*\)"; */\1/' )
ANALYTICS_TRACKING_ID=$( echo "$ANALYTICS_CONSTANT" | \
    sed -e 's/.* = "\(.*\)"; */\1/' )
[ ${#OAUTH_CLIENT_ID} -ne 72 ] && \
    die "OAUTH_CLIENT_ID is not of length 72. Halting."
[ ${#OAUTH_CLIENT_SECRET} -ne 24 ] && \
    die "OAUTH_CLIENT_SECRET is not of length 24. Halting."
[ ${#ANALYTICS_TRACKING_ID} -ne 13 ] && \
    die "ANALYTICS_TRACKING_ID is not of length 13. Halting."

###############################################################################
echo
echo "#"
echo "# Run Eclipse from the command line to generate a new p2 repository from"
echo "# the signed artifacts:"
echo "#"
echo "#     $LOCAL_REPO/artifacts.jar"
echo "#     $LOCAL_REPO/content.jar"
echo "#     $LOCAL_REPO/features/*"
echo "#     $LOCAL_REPO/plugins/*"
echo "#"

set -x
"$ECLIPSE_HOME"/eclipse -nosplash -consolelog \
    -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
    -metadataRepositoryName 'Google Cloud Tools for Eclipse' \
    -metadataRepository file:$LOCAL_REPO \
    -artifactRepositoryName 'Google Cloud Tools for Eclipse' \
    -artifactRepository file:$LOCAL_REPO \
    -source $SIGNED_DIR \
    -publishArtifacts \
    -compress
set +x

if [ ! -e "$LOCAL_REPO/artifacts.jar" -o ! -e "$LOCAL_REPO/content.jar" ]; then
    die "The files have not been generated. Halting."
fi

###############################################################################
echo
echo "#"
echo "# Copy 'gs://gcloud-for-eclipse-testing/metadata.{product,p2.inf}'"
echo "# into '$WORK_DIR'."
echo "#"

set -x
# note that the metadata.p2.inf is renamed p2.inf as required for the p2 ProductPublisher
gsutil cp gs://gcloud-for-eclipse-testing/metadata.product $WORK_DIR
gsutil cp gs://gcloud-for-eclipse-testing/metadata.p2.inf $WORK_DIR/p2.inf
set +x

if [ ! -e "$WORK_DIR/metadata.product" -o ! -e "$WORK_DIR/p2.inf" ]; then
    die "The files were not copied. Halting."
fi

###############################################################################
echo
echo "#"
echo "# Run org.eclipse.equinox.p2.publisher.ProductPublisher to add any"
echo "# additional p2 metadata for the CT4E repository:"
echo "#   - copyright and license have been associated with our public feature"
echo "# Verify using xmllint."
echo "#"

set -x
"$ECLIPSE_HOME"/eclipse \
    -nosplash -console -consolelog \
    -application org.eclipse.equinox.p2.publisher.ProductPublisher \
    -metadataRepository file:$LOCAL_REPO \
    -productFile $WORK_DIR/metadata.product \
    -flavor tooling \
    -append \
    -compress
set +x

# Validate license and copyright
repoName="Google Cloud Tools for Eclipse"
categoryId=com.google.cloud.tools.eclipse.category
featureId=com.google.cloud.tools.eclipse.suite.e45.feature.feature.group
copyrightText="Copyright 2016, 2017 Google Inc."
licenseUri=https://www.apache.org/licenses/LICENSE-2.0
licenseText="Cloud Tools for Eclipse is made available under the Apache\
 License, Version 2.0. Please visit the following URL for details:\
 https://www.apache.org/licenses/LICENSE-2.0"
categoryXPathExpr="/repository[@name='$repoName']/units/unit[@id='$categoryId']"

valid=$(unzip -p $LOCAL_REPO/content.jar \
  | xmllint --xpath \
    "normalize-space(${categoryXPathExpr}/copyright)='$copyrightText' \
        and normalize-space(${categoryXPathExpr}/licenses[@size=1]/license[@uri='$licenseUri'])='$licenseText' \
        and ${categoryXPathExpr}/requires[@size='1']/required/@name='$featureId'" -)
if [ "$valid" != "true" ]; then
    die "$featureId is missing the copyright and license metadata. Halting."
fi

###############################################################################
echo
echo "#"
echo "# Upload the following newly created files"
echo "#"
echo "#     $LOCAL_REPO/artifacts.jar"
echo "#     $LOCAL_REPO/content.jar"
echo "#     $LOCAL_REPO/features/*"
echo "#     $LOCAL_REPO/plugins/*"
echo "#"
echo "# along with"
echo "#"
echo "#     $SIGNED_DIR/index.html"
echo "#"
echo "# into 'gs://cloud-tools-for-eclipse/<VERSION>/'."
echo "#"
echo -n "Enter version: "
read VERSION
ask_proceed

set -x
gsutil cp $LOCAL_REPO/artifacts.jar $LOCAL_REPO/content.jar \
    $SIGNED_DIR/index.html \
    gs://cloud-tools-for-eclipse/$VERSION/ && \
gsutil -m cp -R $LOCAL_REPO/features $LOCAL_REPO/plugins \
    gs://cloud-tools-for-eclipse/$VERSION/
set +x

###############################################################################
echo
echo "#"
echo "# FINAL STEP: MAKING IT PUBLIC"
echo "#"
echo "# Now give the world read permissions to the uploaded files."
echo "#"

set -x
gsutil -m acl ch -r -u AllUsers:R gs://cloud-tools-for-eclipse/$VERSION
set +x

###############################################################################
echo
echo "#"
echo "# Done! Verify that you can install the plugin into Eclipse"
echo "# from the public link:"
echo "#"
echo "# https://storage.googleapis.com/cloud-tools-for-eclipse/$VERSION/"
echo "#"
echo "# If everything is OK, update Lorry."
echo "#"
