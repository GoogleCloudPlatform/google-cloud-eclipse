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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkResolver;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class CloudSdkPreferenceResolverTest {

  private IPreferenceStore preferences;
  File root;
  
  @Before
  public void setUp() {
    // A path that almost certainly does not contain the SDK
    root = File.listRoots()[0];
    preferences = new MockPreferences(root.toString());
  }
  
  @Test
  public void testSetPreferenceInvalid() throws Exception {
    CloudSdkPreferenceResolver resolver = new CloudSdkPreferenceResolver(preferences);
    CloudSdk sdk = new CloudSdk.Builder()
        .resolvers(Collections.singletonList((CloudSdkResolver) resolver)).build();
    assertEquals("SDK should be found at invalid location", root.toPath(), sdk.getSdkPath());
    try {
      sdk.validateCloudSdk();
      fail("root directory should not validate as a valid location");
    } catch (AppEngineException ex) {
      // ignore
    }
  }

  /** Verify that the preference resolver is found by default. */
  @Test
  public void testPreferenceResolverFound() throws Exception {
    List<CloudSdkResolver> resolvers = new CloudSdk.Builder().getResolvers();
    int found = 0;
    for (CloudSdkResolver resolver : resolvers) {
      // Can't just compare classes as class likely loaded from
      // different classloaders
      if (CloudSdkPreferenceResolver.class.getName().equals(resolver.getClass().getName())) {
        found++;
      }
    }
    assertEquals(1, found);
  }

  /** Verify that the preference resolver is not last (that is, overrides PathResolver). */
  @Test
  public void testPreferenceResolverNotLast() throws Exception {
    List<CloudSdkResolver> resolvers = new CloudSdk.Builder().getResolvers();
    // we should have at least our CloudSdkPreferenceResolver, located via ServiceLoader, and
    // the default PathResolver
    assertTrue(resolvers.size() > 1);
    for (int i = 0; i < resolvers.size() - 1; i++) {
      // Can't just compare classes as class likely loaded from
      // different classloaders
      if (CloudSdkPreferenceResolver.class.getName()
          .equals(resolvers.get(i).getClass().getName())) {
        return;
      }
    }
    fail("Could not find " + CloudSdkPreferenceResolver.class);
  }


  private static class MockPreferences implements IPreferenceStore {
    private String stringValue;

    MockPreferences(String stringValue) {
      this.stringValue = stringValue;
    }

    @Override
    public void addPropertyChangeListener(IPropertyChangeListener listener) { 
    }

    @Override
    public boolean contains(String name) {
      return false;
    }

    @Override
    public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
    }

    @Override
    public boolean getBoolean(String name) {
      return false;
    }

    @Override
    public boolean getDefaultBoolean(String name) {
      return false;
    }

    @Override
    public double getDefaultDouble(String name) {
      return 0;
    }

    @Override
    public float getDefaultFloat(String name) {
      return 0;
    }

    @Override
    public int getDefaultInt(String name) {
      return 0;
    }

    @Override
    public long getDefaultLong(String name) {
      return 0;
    }

    @Override
    public String getDefaultString(String name) {
      return null;
    }

    @Override
    public double getDouble(String name) {
      return 0;
    }

    @Override
    public float getFloat(String name) {
      return 0;
    }

    @Override
    public int getInt(String name) {
      return 0;
    }

    @Override
    public long getLong(String name) {
      return 0;
    }

    @Override
    public String getString(String name) {
      return stringValue;
    }

    @Override
    public boolean isDefault(String name) {
      return false;
    }

    @Override
    public boolean needsSaving() {
      return false;
    }

    @Override
    public void putValue(String name, String value) {
    }

    @Override
    public void removePropertyChangeListener(IPropertyChangeListener listener) {      
    }

    @Override
    public void setDefault(String name, double value) {
    }

    @Override
    public void setDefault(String name, float value) {
    }

    @Override
    public void setDefault(String name, int value) {
    }

    @Override
    public void setDefault(String name, long value) {
    }

    @Override
    public void setDefault(String name, String defaultObject) {
    }

    @Override
    public void setDefault(String name, boolean value) {
    }

    @Override
    public void setToDefault(String name) {      
    }

    @Override
    public void setValue(String name, double value) {
    }

    @Override
    public void setValue(String name, float value) {
    }

    @Override
    public void setValue(String name, int value) {
    }

    @Override
    public void setValue(String name, long value) {
    }

    @Override
    public void setValue(String name, String value) {      
    }

    @Override
    public void setValue(String name, boolean value) {      
    }

  }
}
