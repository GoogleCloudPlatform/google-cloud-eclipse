package com.google.cloud.tools.eclipse.appengine.ui;

import org.junit.Assert;
import org.junit.Test;

public class AppEngineImagesTest {

  @Test
  public void testLoadGCP16() {
    Assert.assertNotNull(AppEngineImages.googleCloudPlatform(16));
  }
  
  @Test
  public void testLoadGCP32() {
    Assert.assertNotNull(AppEngineImages.googleCloudPlatform(32));
  }

  @Test
  public void testLoadNonExistentGCPSize() {
    Assert.assertNull(AppEngineImages.googleCloudPlatform(45));
  }

  @Test
  public void testLoadGAE64() {
    Assert.assertNotNull(AppEngineImages.appEngine(64));
  }

}
