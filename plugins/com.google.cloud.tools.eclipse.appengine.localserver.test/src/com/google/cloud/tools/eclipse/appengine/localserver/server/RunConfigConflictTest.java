/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.appengine.api.devserver.DefaultRunConfiguration;
import com.google.cloud.tools.appengine.api.devserver.RunConfiguration;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Tests detection of conflicts between two {@link RunConfiguration}.
 */
public class RunConfigConflictTest {
  @Test
  public void testSameConflict() {
    RunConfiguration config = new DefaultRunConfiguration();
    IStatus status = LocalAppEngineServerLaunchConfigurationDelegate.checkConflicts(config, config,
        StatusUtil.multi(RunConfigConflictTest.class, "Conflict"));
    assertFalse(status.isOK());
    assertThat(status, Matchers.instanceOf(MultiStatus.class));
    IStatus[] children = ((MultiStatus) status).getChildren();
    assertEquals(3, children.length);
    assertEquals("server port: 8080", children[0].getMessage());
    assertEquals("admin port: 8000", children[1].getMessage());
    assertEquals("storage path: <default location>", children[2].getMessage());
  }

  @Test
  public void testNoConflicts() {
    DefaultRunConfiguration config1 = new DefaultRunConfiguration();
    config1.setPort(0); // random allocation
    config1.setAdminPort(0); // random allocation
    config1.setStoragePath("/foo/bar");
    DefaultRunConfiguration config2 = new DefaultRunConfiguration();
    IStatus status = LocalAppEngineServerLaunchConfigurationDelegate.checkConflicts(config1,
        config2, StatusUtil.multi(RunConfigConflictTest.class, "Conflict"));
    assertTrue(status.isOK());
  }
}
