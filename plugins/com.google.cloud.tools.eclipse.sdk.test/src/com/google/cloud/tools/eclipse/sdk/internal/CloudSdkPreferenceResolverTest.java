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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkResolver;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.Test;

public class CloudSdkPreferenceResolverTest {

  @Test
  public void testSetPreferenceInvalid() throws CloudSdkNotFoundException {
    // A path that almost certainly does not contain the SDK
    File root = File.listRoots()[0];
    IPreferenceStore preferences = mock(IPreferenceStore.class);
    when(preferences.getString(anyString())).thenReturn(root.toString());

    CloudSdkPreferenceResolver resolver = new CloudSdkPreferenceResolver(preferences);
    CloudSdk sdk = new CloudSdk.Builder()
        .resolvers(Collections.singletonList((CloudSdkResolver) resolver)).build();
    assertEquals("SDK should be found at invalid location", root.toPath(), sdk.getPath());
    try {
      sdk.validateCloudSdk();
      fail("root directory should not validate as a valid location");
    } catch (AppEngineException ex) {
      // ignore
    }
  }

  /** Verify that the preference resolver is found by default. */
  @Test
  public void testPreferenceResolverFound() {
    List<CloudSdkResolver> resolvers = new CloudSdk.Builder().getResolvers();
    int found = 0;
    for (CloudSdkResolver resolver : resolvers) {
      // Can't just compare classes as class likely loaded from
      // different classloaders
      if (CloudSdkPreferenceResolver.class.getName().equals(resolver.getClass().getName())) {
        found++;
      }
    }
    assertEquals("Could not find CloudSdkPreferenceResolver", 1, found);
  }

  /** Verify that the preference resolver is not last (that is, overrides PathResolver). */
  @Test
  public void testPreferenceResolverNotLast() {
    List<CloudSdkResolver> resolvers = new CloudSdk.Builder().getResolvers();
    // we should have at least our CloudSdkPreferenceResolver, located via ServiceLoader, and
    // the default PathResolver
    assertTrue(resolvers.size() > 1);
    CloudSdkResolver lastResolver = resolvers.get(resolvers.size() - 1);
    assertNotEquals(CloudSdkPreferenceResolver.class.getName(), lastResolver.getClass().getName());
  }
}
