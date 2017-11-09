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

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import java.net.URI;

/**
 * Resolves preference URIs to {@link IPreferenceStore} instances. Preference URIs are of the form
 * <code>instance://org.example.bundle/path/to/boolean</code>. Known schemes are:
 * <ul>
 * <li>instance</li>
 * <li>configuration</li>
 * </ul>
 */
public class PreferenceResolver {

  /**
   * Resolve a path like <code>instance://org.example.bundle/path/to/boolean</code> to an
   * {@link IPreferenceStore}.
   * 
   * @param preferenceUri the preference path
   * @return the corresponding store
   * @throws IllegalArgumentException if unable to resolve the URI
   */
  public static IPreferenceStore resolve(URI preferenceUri) throws IllegalArgumentException {
    IScopeContext context = resolveScopeContext(preferenceUri.getScheme());
    String path = preferenceUri.getHost();
    if (preferenceUri.getPath() != null) {
      path += preferenceUri.getPath();
    }
    return new ScopedPreferenceStore(context, path);
  }

  /**
   * Resolve the given scheme to a preference scope.
   * 
   * @param scheme the preference scheme
   * @return the corresponding preference scope
   * @throws IllegalArgumentException if unable to resolve the scheme to a preference scope
   */
  private static IScopeContext resolveScopeContext(String scheme) throws IllegalArgumentException {
    switch (scheme) {
      case "instance":
        return InstanceScope.INSTANCE;
      case "configuration":
        return ConfigurationScope.INSTANCE;
      default:
        throw new IllegalArgumentException("Unknown scheme: " + scheme);
    }
  }
}
