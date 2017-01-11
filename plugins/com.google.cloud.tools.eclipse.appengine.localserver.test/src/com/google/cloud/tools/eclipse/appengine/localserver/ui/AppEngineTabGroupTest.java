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

import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.junit.Assert;
import org.junit.Test;

public class AppEngineTabGroupTest {
  
  @Test
  public void testCreateTabs() {
    AppEngineTabGroup group = new AppEngineTabGroup();
    group.createTabs(null, "");
    for (ILaunchConfigurationTab tab : group.getTabs()) {
      Assert.assertNotNull(tab);
    }
    Assert.assertEquals("Server", group.getTabs()[0].getName());
    Assert.assertEquals("Arguments", group.getTabs()[1].getName());
  }

}
