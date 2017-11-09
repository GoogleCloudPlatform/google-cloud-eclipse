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

package com.google.cloud.tools.eclipse.preferences.areas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Test {@link PreferenceResolver}.
 */
public class PreferenceResolverTest {

  @Test
  public void testResolveInstance() throws IllegalArgumentException, URISyntaxException {
    IPreferenceStore store = PreferenceResolver.resolve(new URI("instance://com.google.test/foo"));
    assertTrue(store instanceof ScopedPreferenceStore);

    IEclipsePreferences[] nodes = ((ScopedPreferenceStore) store).getPreferenceNodes(false);
    assertEquals(1, nodes.length);
    assertEquals("/instance/com.google.test/foo", nodes[0].absolutePath());
  }

  @Test
  public void testResolveConfig() throws IllegalArgumentException, URISyntaxException {
    IPreferenceStore store =
        PreferenceResolver.resolve(new URI("configuration://com.google.test/foo"));
    assertTrue(store instanceof ScopedPreferenceStore);

    IEclipsePreferences[] nodes = ((ScopedPreferenceStore) store).getPreferenceNodes(false);
    assertEquals(1, nodes.length);
    assertEquals("/configuration/com.google.test/foo", nodes[0].absolutePath());
  }
}
