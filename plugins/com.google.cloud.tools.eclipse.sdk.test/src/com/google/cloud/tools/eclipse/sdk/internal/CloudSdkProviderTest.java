/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.sdk.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.sdk.CloudSdkProvider;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

public class CloudSdkProviderTest {

  private IPreferenceStore preferences;
  
  @Before
  public void setUp() {
    preferences = new MockPreferences();
  }
  
  /** Verify that the preference overrides auto discovery. */
  @Test
  public void testSetPreferenceInvalid() throws Exception {
    // A path that almost certainly does not contain the SDK
    File root = File.listRoots()[0];

    CloudSdk.Builder builder = new CloudSdkProvider(preferences).createBuilder();
    // todo we shouldn't need reflection here; use visible for testing if we must
    assertEquals(root.toPath(), ReflectionUtil.getField(builder, "sdkPath", Path.class));
    CloudSdk instance = builder.build();
    assertEquals(root.toPath(), instance.getSdkPath());
    try {
      instance.validate();
      fail("root directory should not be a valid location");
    } catch (AppEngineException ex) {
      // ignore
    }
  }
  
  private static class MockPreferences implements IPreferenceStore {

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
      // A path that almost certainly does not contain the SDK
      return File.listRoots()[0].toString();
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
