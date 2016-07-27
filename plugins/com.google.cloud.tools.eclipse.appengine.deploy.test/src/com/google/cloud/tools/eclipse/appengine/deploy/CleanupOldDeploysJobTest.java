package com.google.cloud.tools.eclipse.appengine.deploy;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Test;

public class CleanupOldDeploysJobTest {

  @Test
  public void testRun_withNoDirectories() throws InterruptedException, IOException {
    testRun(0, new String[0]);
  }

  @Test
  public void testRun_withOneDirectory() throws InterruptedException, IOException {
    testRun(1, new String[] {"1"});
  }

  @Test
  public void testRun_withTwoDirectories() throws InterruptedException, IOException {
    testRun(2, new String[] {"1", "2"});
  }

  @Test
  public void testRun_withThreeDirectories() throws InterruptedException, IOException {
    testRun(3, new String[] {"1", "2"});
  }

  private void testRun(int directoryCount, String[] expectedDirectoriesToKeep) throws InterruptedException, IOException {
    Path tempDirectory = Files.createTempDirectory("cleanupolddeploysjobtest");
    tempDirectory.toFile().deleteOnExit();
    createTestDirectories(tempDirectory, directoryCount);

    IPath tempDirectoryPath = new org.eclipse.core.runtime.Path(tempDirectory.toAbsolutePath().toString());
    CleanupOldDeploysJob job = new CleanupOldDeploysJob(tempDirectoryPath);
    job.run(mock(IProgressMonitor.class));

    File[] directoriesKept = tempDirectoryPath.toFile().listFiles();
    Arrays.sort(directoriesKept, new Comparator<File>() {

      @Override
      public int compare(File o1, File o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    assertThat(directoriesKept.length, is(expectedDirectoriesToKeep.length));
    for (int i = 0; i < expectedDirectoriesToKeep.length; i++) {
      assertThat(directoriesKept[i].getName(), is(expectedDirectoriesToKeep[i]));
    }
  }

  private void createTestDirectories(Path tempDirectory, int count) throws InterruptedException, IOException {
    long now = System.currentTimeMillis();
    // most recent directory will be "1", oldest is "<count>"
    for (int i = count; i > 0; --i) {
      Path path = tempDirectory.resolve(Integer.toString(i));
      Files.createDirectories(path);
      Files.setLastModifiedTime(path, FileTime.fromMillis(now - i * 1000L)); // to ensure correct ordering
    }
  }

}
