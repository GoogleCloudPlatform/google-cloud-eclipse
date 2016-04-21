package com.google.cloud.tools.eclipse.appengine.localserver.util;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.google.cloud.tools.eclipse.util.OSUtilities;
import com.google.cloud.tools.eclipse.util.ProcessUtilities;
import com.google.common.collect.Lists;

public class ProcessUtilitiesTest {

  @Test
  public void testlaunchProcessAndWaitFor_additionalPathAdded() throws InterruptedException, IOException {
    if (OSUtilities.isMac() || OSUtilities.isUnix()) {
      List<String> commands = Lists.newArrayList("/bin/bash", "-c", "echo $PATH");
      File workingDir = new File("/tmp");
      List<String> additionalPaths = Lists.newArrayList("/tmp/foo", "/tmp/bar");
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ProcessUtilities.launchProcessAndWaitFor(commands, workingDir, additionalPaths, outputStream);
      String result = outputStream.toString("UTF-8");
      assertThat(result, containsString("/tmp/foo"));
      assertThat(result, containsString("/tmp/bar"));
    }
  }
}
