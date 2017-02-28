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

package com.google.cloud.tools.eclipse.util;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudToolsInfoTest {
  @Mock
  public IBundleGroupProvider bundleGroupProvider;
  @Mock
  public IBundleGroup bundleGroup;

  @Test
  public void testGetToolsVersion_featureId() {
    Assert.assertEquals("com.google.cloud.tools.eclipse.suite.e45.feature",
        CloudToolsInfo.CLOUD_TOOLS_FOR_ECLIPSE_FEATURE_ID);
  }

  @Test
  public void testGetToolsVersion_noFeature() {
    Assert.assertEquals("0.0.0", CloudToolsInfo.getToolsVersion(new IBundleGroupProvider[0]));
  }

  @Test
  public void testGetToolsVersion_hasFeature() {
    Mockito.when(bundleGroupProvider.getBundleGroups())
        .thenReturn(new IBundleGroup[] {bundleGroup});
    Mockito.when(bundleGroup.getIdentifier())
        .thenReturn(CloudToolsInfo.CLOUD_TOOLS_FOR_ECLIPSE_FEATURE_ID);
    Mockito.when(bundleGroup.getVersion()).thenReturn("123.456.789");
    Assert.assertEquals("123.456.789",
        CloudToolsInfo.getToolsVersion(new IBundleGroupProvider[] {bundleGroupProvider}));
  }

  @Test
  public void testUserAgent() {
    Assert.assertTrue(CloudToolsInfo.USER_AGENT.startsWith("gcloud-eclipse-tools/"));
  }
}
