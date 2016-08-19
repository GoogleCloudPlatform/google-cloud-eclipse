/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.appengine.localserver;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;

public class PreferencesInitializer extends AbstractPreferenceInitializer {

  public static final String LAUNCH_BROWSER = "launchBrowser";

  @Override
  public void initializeDefaultPreferences() {
    DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID).putBoolean(LAUNCH_BROWSER, true);
  }
}
