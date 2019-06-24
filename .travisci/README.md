# Continuous Integration Support for Travis

This directory contains files and helpers for Travis-CI.

## Toolchains

The [`toolchains.xml`](toolchains.xml) file contains Maven toolchains descriptions
for the installations found on the Travis build images.  They may need to be updated
as changes are made to the Travis images.

## Uploading Build Reports to GCS

The .travis.yml is configured to upload build reports and test
artifacts, like SWTBot `screenshots/` and `surefire-reports/`, to
a GCS bucket.  These artifacts are publicly accessible (via
`gsutil cp -a public-read`).  The bucket can be accessed at:

   https://console.cloud.google.com/storage/browser/BUCKET

Configuring the uploading requires creating a GCS bucket, and
creating a service account with the _Storage Object Creator_ role.

### Configuring Auto-Deletion for the GCS Bucket

`gcs.lifecycle.json` provides an [auto-deletion configuration
policy](https://cloud.google.com/storage/docs/managing-lifecycles#delete_an_object)
suitable for a GCS bucket hosting the build reports.  It should be
installed with:

```
$ gsutil lifecycle set .travisci/gcs.lifecycle.json gs://BUCKET
```

### Configuring the Service Account Key

  1. Create a key for the service account from the [Cloud
     Console](https://console.cloud.google.com/iam-admin/serviceaccounts)
     with role _Storage > Storage Object Creator_.
     Check the _Furnish a new private key_.  Click _Create_.
     This will produce a JSON file; rename it to `travis-service-account.json`.

  2. Follow Travis-CI's [instructions for automatically encrypting
     files](https://docs.travis-ci.com/user/encrypting-files/) to encrypt
     `travis-service-account.json` and place the result in
     `.travisci/travis-service-account.json.enc`.
     Replace the `$encrypted_XXXX_*` values in the `.travis.yml`.

  3. Commit the result and push.  Verify that the next build
     pushes build bits to the configured bucket.
