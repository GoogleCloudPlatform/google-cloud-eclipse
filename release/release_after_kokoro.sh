#!/bin/sh

# This script starts with Step 14 in the release process (i.e., after Kokoro
# has built and signed the binaries.)
#
# It is safe to rerun this script again when something goes wrong, provided
# that you didn't complete the final step, which means you are done. (The final
# step grants public access to the files uploaded to cloud-tools-for-eclipse.)
#
# Note: this script clears and creates "$HOME/CT4E_release_work" (after
# confirmation) for a temporary work directory, which can be inspected later
# and/or deleted safely anytime.

die() {
    for line in "$@"; do
        echo $line
    done
    exit 1
}

[ -z "$ECLIPSE_HOME" ] && \
    die 'The release steps require running an Eclipse binary.' \
        'Set $ECLIPSE_HOME before running this script.'
[ ! -x "$ECLIPSE_HOME/eclipse" ] && \
    die "'$ECLIPSE_HOME/eclipse' is not an executable. Halting."

WORK_DIR=$HOME/CT4E_release_work
SIGNED_DIR=$WORK_DIR/signed
LOCAL_REPO=$WORK_DIR/repository

ask_proceed() {
    while true; do
        echo -n "Proceed ([Y]/n)? "
        read ANSWER
        [ "$ANSWER" == 'n' -o "$ANSWER" == 'N' ] && exit 1
        [ "$ANSWER" == 'y' -o "$ANSWER" == 'Y' -o -z "$ANSWER" ] && break
        echo -n "Invalid answer. "
    done
}

###############################################################################
echo "#"
echo "# Clear '$WORK_DIR'."
echo "#"
ask_proceed

set -x
rm -rf $WORK_DIR && mkdir $WORK_DIR
set +x

###############################################################################
echo
echo "#"
echo "# Copy files built (signed) by Kokoro"
echo "# into '$SIGNED_DIR'."
echo "#"

echo -n "Enter GCS bucket URL taken from the Kokoro page: "
read GCS_URL
GCS_BUCKET_URL=$( echo $GCS_URL | \
    sed -e 's,.*\(signedjars/staging/prod/google-cloud-eclipse/ubuntu/release/[0-9]\+/[0-9-]\+\),\1,' )
echo "GCS URL extracted: $GCS_BUCKET_URL"
echo -n "Check if the URL is correct. "
ask_proceed

set -x
mkdir $SIGNED_DIR && \
    gsutil -m cp -R gs://$GCS_BUCKET_URL/gfile/signed/* $SIGNED_DIR
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
ask_proceed

LOGIN_CONSTANTS=$( javap -private -classpath \
    $SIGNED_DIR/plugins/com.google.cloud.tools.eclipse.appengine.login_*.jar \
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

echo
echo -n "Looks good, but check the output above once more. "
ask_proceed

###############################################################################
echo
echo "#"
echo "# Run Eclipse from the command line to generate the following files:"
echo "#"
echo "#     $LOCAL_REPO/artifacts.xml"
echo "#     $LOCAL_REPO/content.xml"
echo "#     $LOCAL_REPO/features/*"
echo "#     $LOCAL_REPO/plugins/*"
echo "#"
ask_proceed

set -x
$ECLIPSE_HOME/eclipse -nosplash -consolelog \
    -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
    -metadataRepositoryName 'Google Cloud Tools for Eclipse' \
    -metadataRepository file:$LOCAL_REPO \
    -artifactRepositoryName 'Google Cloud Tools for Eclipse' \
    -artifactRepository file:$LOCAL_REPO \
    -source $SIGNED_DIR \
    -publishArtifacts
set +x

if [ ! -e "$LOCAL_REPO/artifacts.xml" -o ! -e "$LOCAL_REPO/content.xml" ]; then
    die "The files have not been generated. Halting."
fi

###############################################################################
echo
echo "#"
echo "# Copy 'gs://gcloud-for-eclipse-testing/category.xml'"
echo "# into '$WORK_DIR'."
echo "#"
ask_proceed

set -x
gsutil cp gs://gcloud-for-eclipse-testing/category.xml $WORK_DIR
set +x

if [ ! -e "$WORK_DIR/category.xml" ]; then
    die "The file was not copied. Halting."
fi

###############################################################################
echo
echo "#"
echo "# Run org.eclipse.equinox.p2.publisher.CategoryPublisher."
echo "#"
ask_proceed

set -x
$ECLIPSE_HOME/eclipse \
    -nosplash -console -consolelog \
    -application org.eclipse.equinox.p2.publisher.CategoryPublisher \
    -metadataRepository file:$LOCAL_REPO \
    -categoryDefinition file:$WORK_DIR/category.xml \
    -categoryQualifier
set +x

###############################################################################
echo
echo "#"
echo "# Upload the following newly created files"
echo "#"
echo "#     $LOCAL_REPO/artifacts.xml"
echo "#     $LOCAL_REPO/content.xml"
echo "#     $LOCAL_REPO/features/*"
echo "#     $LOCAL_REPO/plugins/*"
echo "#"
echo "# into 'gs://cloud-tools-for-eclipse/<VERSION>/'."
echo "#"
echo -n "Enter version: "
read VERSION
ask_proceed

set -x
gsutil cp $LOCAL_REPO/artifacts.xml $LOCAL_REPO/content.xml \
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
ask_proceed

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
