package com.google.cloud.tools.eclipse.appengine.deploy;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.Test;

import com.google.common.io.Files;

public class CleanupOldDeploysJobTest {

  @Test
  public void testRun_withNoDirectories() throws InterruptedException {
    testRun(0, new String[0]);
  }

  @Test
  public void testRun_withOneDirectory() throws InterruptedException {
    testRun(1, new String[] {"1"});
  }

  @Test
  public void testRun_withTwoDirectories() throws InterruptedException {
    testRun(2, new String[] {"1", "2"});
  }

  @Test
  public void testRun_withThreeDirectories() throws InterruptedException {
    testRun(3, new String[] {"1", "2"});
  }

  private void testRun(int directoryCount, String[] expectedDirectoriesToKeep) throws InterruptedException {
    File tempDirectory = Files.createTempDir();
    tempDirectory.deleteOnExit();
    IPath tempDirectoryPath = new Path(tempDirectory.getAbsolutePath());
    createTestDirectories(tempDirectoryPath, directoryCount);

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

  private void createTestDirectories(IPath tempDir, int count) throws InterruptedException {
    // most recent directory will be "1", oldest is "<count>"
    for (int i = count; i > 0; --i) {
      assertTrue(new File(tempDir.append(Integer.toString(i)).toOSString()).mkdir());
      Thread.sleep(1000L); // we need at least 1 second to have different modification times
    }
  }

}
