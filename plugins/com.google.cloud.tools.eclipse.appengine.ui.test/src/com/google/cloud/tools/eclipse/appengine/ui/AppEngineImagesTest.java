package com.google.cloud.tools.eclipse.appengine.ui;

import org.junit.Assert;
import org.junit.Test;

public class AppEngineImagesTest {

  @Test
  public void testLoad16() {
    Assert.assertNotNull(AppEngineImages.googleCloudPlatform(16));
  }
  
  @Test
  public void testLoad32() {
    Assert.assertNotNull(AppEngineImages.googleCloudPlatform(32));
  }

  @Test
  public void testLoadNonExistentSize() {
    Assert.assertNull(AppEngineImages.googleCloudPlatform(45));
  }

}
