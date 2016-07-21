package com.google.cloud.tools.eclipse.util.io;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

import org.junit.Test;

public class DeleteAllVisitorTest {

  @Test
  public void test_nonEmptyDirectory() throws IOException {
    File tempDirectory = com.google.common.io.Files.createTempDir();
    Files.createTempFile(tempDirectory.toPath(), "deleteallvisitortest", null);
    Path childDirectory = Files.createTempDirectory(tempDirectory.toPath(), "deleteallvisitortest", new FileAttribute<?>[0]);
    Files.createTempFile(childDirectory, "deleteallvisitortest", null);

    Files.walkFileTree(tempDirectory.toPath(), new DeleteAllVisitor());

    assertFalse(tempDirectory.exists());
  }

}
