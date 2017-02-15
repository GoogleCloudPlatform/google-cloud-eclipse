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
