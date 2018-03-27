/*
 * Copyright 2018 Google LLC
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

package com.google.cloud.tools.eclipse.appengine.libraries;

import org.eclipse.core.runtime.CoreException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BomTest {

  private Bom cloudBom;
  
  @Before
  public void setUp() throws CoreException {
    cloudBom = Bom.loadBom("com.google.cloud", "google-cloud-bom", "0.41.0-alpha", null);
  }

  @Test
  public void testCloudSpeech() {
    Assert.assertTrue(cloudBom.defines("com.google.cloud", "google-cloud-speech"));
  }

  @Test
  public void testGroupdId() {
    Assert.assertFalse(cloudBom.defines("com.google", "google-cloud-speech"));
  }
  
  @Test
  public void testCloudVision() {
    Assert.assertTrue(cloudBom.defines("com.google.cloud", "google-cloud-vision"));
  }
  
  @Test
  public void testUnknown() {
    Assert.assertFalse(cloudBom.defines("com.google.cloud", "unknown"));
  }

  @Test
  public void testGuava() {
    Assert.assertFalse(cloudBom.defines("com.google.guava", "guava"));
  }
  
}
