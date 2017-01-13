#!/bin/sh

# This script starts with Step 14 in the release process (i.e., after Kokoro
# has built and signed the binaries.)
#
# It is safe to re-run this script again when something goes wrong, provided
# that you didn't complete the final step, which means you are done. (The final
# step grants public access to files uploaded to cloud-tools-for-eclipse.)
#
# Note: this script clears and creates "$HOME/CT4E_release_work" (after
# confirmation) for temporary work directory, which can be inspected later
# and/or deleted safely anytime.

if [ -z "$ECLIPSE_HOME" ]; then
    echo 'The release steps require running an Eclipse binary.'
    echo 'Set $ECLIPSE_HOME before running this script.'
    exit 1
elif [ ! -x "$ECLIPSE_HOME/eclipse" ]; then
    echo "'$ECLIPSE_HOME/eclipse' is not an executable. Halting."
    exit 1
fi

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
GCS_BUCKET_URL=$( echo $GCS_URL | sed -e 's,.*\(signedjars/staging/prod/google-cloud-eclipse/ubuntu/release/[0-9]\+/[0-9-]\+\),\1,' )
echo "GCS URL extracted: $GCS_BUCKET_URL"
echo -n "Check if the URL is correct. "
ask_proceed

set -x
mkdir $SIGNED_DIR && \
    gsutil -m cp -R gs://$GCS_BUCKET_URL/gfile/signed/* $SIGNED_DIR
set +x

if [ 0 -eq $( ls -1 $SIGNED_DIR | wc -l ) ]; then
    echo "'$SIGNED_DIR' is empty. Wrong GCS bucket URL provided?"
    echo "Fix the problem and rerun the script. Halting."
    exit 1
fi

###############################################################################
echo
echo "#"
echo "# Verify if constants have been injected..."
echo "#"
ask_proceed

set -x
javap -private -classpath $SIGNED_DIR/plugins/com.google.cloud.tools.eclipse.appengine.login_0.1.0.*.jar \
       -constants com.google.cloud.tools.eclipse.appengine.login.Constants
javap -classpath $SIGNED_DIR/plugins/com.google.cloud.tools.eclipse.usagetracker_0.1.0.*.jar \
       -constants com.google.cloud.tools.eclipse.usagetracker.Constants
set +x

echo "Check the output above. "
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
    echo "The files have not been generated. Halting."
    exit 1
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
    echo "The file was not copied. Halting."
    exit 1
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
