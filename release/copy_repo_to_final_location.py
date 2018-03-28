#!/usr/bin/env python2

"""Copies a Kokoro-built CT4E release to a permenant location and makes it
publicly available.

The GCS location of a Kokoro-built repo is not suitable for direct release or
repo management. An example location:

    gs://kokoro-ct4e-release/prod/google-cloud-eclipse/ubuntu/jar_signing/34/20180323-215548

As a final step, we need to copy such a repo to a permanet location, e.g.:

    gs://cloud-tools-for-eclipse/1.6.1
"""

import re
import subprocess
import sys


def _AskRepoOrigin():
  print
  print "#"
  print "# Enter the GCS URL of the Kokoro-built repo."
  print '# ("Artficat location" in the "jar_signing" success email.)'
  gcs_url = raw_input("URL? ")

  match_obj = re.search(
      r'kokoro-ct4e-release/prod/google-cloud-eclipse/ubuntu/jar_signing/\d+/[\d-]+$',
      gcs_url)
  if match_obj:
    return "gs://" + match_obj.group()
  else:
    print "Wrong URL. Try again."
    return _AskRepoOrigin()


def _AskVersion():
  print
  print "#"
  print "# Enter the CT4E version (e.g., 9.9.9)."
  version = raw_input("Version? ")

  match_obj = re.search(r'^\d+\.\d+\.\d+$', version)
  if match_obj:
    return version
  else:
    print "Wrong format. Try again."
    return _AskVersion()


def _GcsLocationExists(gcs_location):
  try:
    subprocess.check_output(
        ["gsutil", "ls", gcs_location], stderr=subprocess.STDOUT)
    return True
  except subprocess.CalledProcessError:
    return False


def main(argv):
  gcs_origin = _AskRepoOrigin()
  version = _AskVersion()
  gcs_destination = "gs://cloud-tools-for-eclipse/" + version

  if _GcsLocationExists(gcs_destination):
    print
    print "#"
    print "# The destination directory already exists. If the version you"
    print "# entered is correct, delete it first and try again."
    print "#"
    print "# Command to delete:"
    print "# gsutil -m rm " + gcs_destination + "/**"
    exit(1)

  # Copy the repo
  subprocess.check_call(
      ["gsutil", "-m", "cp", "-R", gcs_origin, gcs_destination])

  # Make it publicly available
  subprocess.check_call(
      ["gsutil", "-m", "acl", "ch", "-R", "-u", "AllUsers:R", gcs_destination])

  print
  print "#"
  print "# Copy done! The repo URL for installation:"
  print "#"
  print "# https://storage.googleapis.com/cloud-tools-for-eclipse/" + version


if __name__ == "__main__":
   main(sys.argv[1:])
