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

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerLaunchConfigurationDelegate;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.commands.IParameterValues;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchMode;

/**
 * A helper class for the Eclipse UI that provides completions for the parameters for
 * LocalAppEngineServer*-supported launch modes.
 */
public class LaunchModes implements IParameterValues {
  @Override
  @SuppressWarnings("rawtypes")
  public Map getParameterValues() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    Map<String, String> modes = new HashMap<>();
    for (String modeId : LocalAppEngineServerLaunchConfigurationDelegate.SUPPORTED_LAUNCH_MODES) {
      ILaunchMode mode = manager.getLaunchMode(modeId);
      if (mode != null) {
        // label is intended to be shown in menus and buttons and often has
        // embedded '&' for mnemonics, which isn't useful here
        String label = mode.getLabel();
        label = label.replace("&", "");
        modes.put(label, mode.getIdentifier());
      }
    }
    return modes;
  }
}
