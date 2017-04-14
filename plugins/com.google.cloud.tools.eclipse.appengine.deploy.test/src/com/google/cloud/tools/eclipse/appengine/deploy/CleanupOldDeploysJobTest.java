/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.deploy;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Comparator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CleanupOldDeploysJobTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testRun_withNoDirectories() throws IOException {
    testRun(0, new String[0]);
  }

  @Test
  public void testRun_withOneDirectory() throws IOException {
    testRun(1, new String[] {"1"});
  }

  @Test
  public void testRun_withTwoDirectories() throws IOException {
    testRun(2, new String[] {"1", "2"});
  }

  @Test
  public void testRun_withThreeDirectories() throws IOException {
    testRun(3, new String[] {"1", "2"});
  }

  private void testRun(int directoryCount, String[] expectedDirectoriesToKeep) throws IOException {
    createTestDirectories(directoryCount);

    IPath tempFolderPath = new Path(tempFolder.getRoot().toString());
    CleanupOldDeploysJob job = new CleanupOldDeploysJob(tempFolderPath);
    job.run(mock(IProgressMonitor.class));

    File[] directoriesKept = tempFolder.getRoot().listFiles();
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

  private void createTestDirectories(int count) throws IOException {
    long now = System.currentTimeMillis();
    // most recent directory will be "1", oldest is "<count>"
    for (int i = count; i > 0; --i) {
      File newFolder = tempFolder.newFolder(Integer.toString(i));
      Files.setLastModifiedTime(
          newFolder.toPath(), FileTime.fromMillis(now - i * 1000L)); // to ensure correct ordering
    }
  }

}
