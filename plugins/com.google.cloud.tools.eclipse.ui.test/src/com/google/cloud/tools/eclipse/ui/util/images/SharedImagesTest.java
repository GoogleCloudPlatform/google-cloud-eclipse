/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.ui.util.images;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class SharedImagesTest {

  @Test
  public void testCreateCloudSdkImage() {
    assertNotNull(SharedImages.CLOUDSDK_IMAGE_DESCRIPTOR.getImageData());
  }

  @Test
  public void testCreateGcpImage() {
    assertNotNull(SharedImages.GCP_IMAGE_DESCRIPTOR.getImageData());
  }

  @Test
  public void testCreateGreyGcpImage() {
    assertNotNull(SharedImages.GCP_GREY_IMAGE_DESCRIPTOR.getImageData());
  }

  @Test
  public void testCreateGreyProjectImage() {
    assertNotNull(SharedImages.PROJECT_GREY_IMAGE_DESCRIPTOR.getImageData());
  }

  @Test
  public void testCreateGcpWizardImage() {
    assertNotNull(SharedImages.GCP_WIZARD_IMAGE_DESCRIPTOR.getImageData());
  }

  @Test
  public void testCreateGreyDatastoreIcon() {
    assertNotNull(SharedImages.DATASTORE_GREY_IMAGE_DESCRIPTOR.getImageData());
  }

  @Test
  public void testCreateRefreshIcon() {
    assertNotNull(SharedImages.REFRESH_IMAGE_DESCRIPTOR.getImageData());
  }

}
