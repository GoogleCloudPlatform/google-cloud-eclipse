package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.junit.Test;

import com.google.cloud.tools.eclipse.appengine.deploy.standard.DialogStagingDirectoryProvider;

public class DialogStagingDirectoryProviderTest {

  @Test(expected = IllegalArgumentException.class)
  public void testGetStagingDirectory_nullShell() {
    new DialogStagingDirectoryProvider(null).get();
  }

}
