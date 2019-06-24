#!/usr/bin/env python

import unittest
from copy_repo_to_final_location import _IsVersionString
from copy_repo_to_final_location import _FormatGcsUrl
from copy_repo_to_final_location import _GcsLocationExists
from copy_repo_to_final_location import _GetCopyCommand
from copy_repo_to_final_location import _GetPublicAccessCommand


class CopyRepoToFinalLocationTest(unittest.TestCase):

  def testIsVersionString_WrongInput(self):
    self.assertFalse(_IsVersionString("invalid version"))

  def testIsVersionString(self):
    self.assertTrue(_IsVersionString("1.6.0"));

  def testIsVersionString_WholeString(self):
    self.assertFalse(_IsVersionString(" 1.6.0"));
    self.assertFalse(_IsVersionString("1.6.0 "));

  def testIsVersionString_MultiDigit(self):
    self.assertTrue(_IsVersionString("12.345.67"));

  def testFormatGcsUrl_WrongInput(self):
    self.assertFalse(_FormatGcsUrl("invalid URL"))

  def testFormatGcsUrl_MinimalUrl(self):
    self.assertEquals("gs://kokoro-ct4e-release/prod/google-cloud-eclipse/ubuntu/jar_signing/34/20180323-215548",
        _FormatGcsUrl("kokoro-ct4e-release/prod/google-cloud-eclipse/ubuntu/jar_signing/34/20180323-215548"))

  def testFormatGcsUrl_TrailingSlash(self):
    self.assertFalse(_FormatGcsUrl("kokoro-ct4e-release/prod/google-cloud-eclipse/ubuntu/jar_signing/34/20180323-215548/"))

  def testFormatGcsUrl_InvalidJobNumber(self):
    self.assertFalse(_FormatGcsUrl("kokoro-ct4e-release/prod/google-cloud-eclipse/ubuntu/jar_signing/number/20180323-215548"))

  def testFormatGcsUrl_InvalidTimestamp(self):
    self.assertFalse(_FormatGcsUrl("kokoro-ct4e-release/prod/google-cloud-eclipse/ubuntu/jar_signing/34/timestamp"))

  def testFormatGcsUrl_GcslUrl(self):
    self.assertEquals("gs://kokoro-ct4e-release/prod/google-cloud-eclipse/ubuntu/jar_signing/13/20180322-160751",
        _FormatGcsUrl("gs://kokoro-ct4e-release/prod/google-cloud-eclipse/ubuntu/jar_signing/13/20180322-160751"))

  def testFormatGcsUrl_CloudConsoleUrl(self):
    self.assertEquals("gs://kokoro-ct4e-release/prod/google-cloud-eclipse/ubuntu/jar_signing/21/20180323-101656",
        _FormatGcsUrl("https://console.cloud.google.com/storage/browser/kokoro-ct4e-release/prod/google-cloud-eclipse/ubuntu/jar_signing/21/20180323-101656"))

  def testGcsLocationExists_ExistingLocation(self):
    self.assertTrue(_GcsLocationExists("gs://cloud-tools-for-eclipse/1.6.0"))

  def testGcsLocationExists_NonExistingLocation(self):
    self.assertEqual(False,
        _GcsLocationExists("gs://cloud-tools-for-eclipse/non-existing"))

  def testGetCopyCommand(self):
    self.assertEquals(["gsutil", "-m", "cp", "-R", "origin", "destination"],
        _GetCopyCommand("origin", "destination"))

  def testGetPublicAccessCommand(self):
    self.assertEquals(
        ["gsutil", "-m", "acl", "ch", "-R", "-u", "AllUsers:R", "target"],
        _GetPublicAccessCommand("target"))


if __name__ == '__main__':
  unittest.main()
