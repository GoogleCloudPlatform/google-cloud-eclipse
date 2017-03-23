/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link EclipsePreferenceStore}.
 */
@RunWith(JUnit4.class)
public class EclipsePreferenceStoreTest {
  @Mock
  private IEclipsePreferences eclipsePrefs;

  private EclipsePreferenceStore prefs;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    prefs = new EclipsePreferenceStore(eclipsePrefs);
  }

  @Test
  public void testGetOptionReturnsEclipseValue() {
    when(eclipsePrefs.get("myOption", null)).thenReturn("bar");

    assertEquals("bar", prefs.getOption("myOption"));
  }

  @Test
  public void testGetOptionWithNullReturnsEmptyOptional() {
    when(eclipsePrefs.get("myOption", null)).thenReturn(null);

    assertNull(prefs.getOption("myOption"));
  }

  @Test
  public void testSetOptionSetsValue() {
    prefs.setOption("myOption", "myProj");

    verify(eclipsePrefs).put("myOption", "myProj");
  }

  @Test
  public void testSaveFlushesPreferences() throws Exception {
    prefs.save();

    verify(eclipsePrefs).flush();
  }
}
