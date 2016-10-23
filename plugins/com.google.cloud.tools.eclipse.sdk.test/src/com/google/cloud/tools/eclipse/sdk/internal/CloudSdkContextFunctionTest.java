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

package com.google.cloud.tools.eclipse.sdk.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.io.IOException;
import java.nio.file.Path;

// TODO important: these tests need to not touch global static state
public class CloudSdkContextFunctionTest {
  private IEclipseContext context;
  private Path mockSdk;

  /** Set up. */
  @Before
  public void setUp() {
    BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
    context = EclipseContextFactory.getServiceContext(bundleContext);
    assertNotNull(context);
  }

  /** Tear down. */
  @After
  public void tearDown() throws IOException {
    context.dispose();
    if (mockSdk != null) {
      MockSdkGenerator.deleteMockSdk(mockSdk);
    }
  }

  /** Retrieving with an invalid path should return NOT_A_VALUE. */
  @Ignore
  public void testNoLocationFails() {
    CloudSdkContextFunction function = new CloudSdkContextFunction();
    context.set(PreferenceConstants.CLOUDSDK_PATH, "path/does/not/exist");
    Object instance = function.compute(context, CloudSdk.class.getName());
    assertEquals(CloudSdkContextFunction.NOT_A_VALUE, instance);
  }

  @Test
  public void testRetrieveWithLocation() throws Exception {
    mockSdk = MockSdkGenerator.createMockSdk();
    context.set(PreferenceConstants.CLOUDSDK_PATH, mockSdk.toString());
    CloudSdkContextFunction function = new CloudSdkContextFunction();

    Object instance = function.compute(context, CloudSdk.class.getName());
    assertNotNull(instance);
    assertEquals(CloudSdk.class, instance.getClass());
    assertEquals(mockSdk, ((CloudSdk) instance).getSdkPath());
  }

  @Ignore("affected from changes in global state")
  public void testContextFunctionReinvoked() throws Exception {
    context.set(PreferenceConstants.CLOUDSDK_PATH, "path/does/not/exist");
    CloudSdk instance = context.get(CloudSdk.class);
    assertNull(instance);

    mockSdk = MockSdkGenerator.createMockSdk();
    // setting CLOUDSDK_PATH should cause any previously computed results to be recomputed
    context.set(PreferenceConstants.CLOUDSDK_PATH, mockSdk.toString());
    instance = context.get(CloudSdk.class);
    assertNotNull(instance);
    assertEquals(mockSdk, ReflectionUtil.getField(instance, "sdkPath", Path.class));
  }

}
