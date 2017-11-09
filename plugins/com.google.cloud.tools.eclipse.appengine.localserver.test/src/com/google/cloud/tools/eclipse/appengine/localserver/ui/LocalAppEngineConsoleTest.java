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

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour;
import org.junit.Assert;
import org.junit.Test;

public class LocalAppEngineConsoleTest {
  @Test
  public void testGetServerBehaviourDelegate_noDelegate() {
    LocalAppEngineConsole console = new LocalAppEngineConsole.Factory(null).createConsole("test");
    Assert.assertNull(console.getServerBehaviourDelegate());
  }

  @Test
  public void testGetServerBehaviourDelegate_withDelegate() {
    LocalAppEngineServerBehaviour delegate = new LocalAppEngineServerBehaviour();
    LocalAppEngineConsole console =
        new LocalAppEngineConsole.Factory(delegate).createConsole("test");
    Assert.assertEquals(delegate, console.getServerBehaviourDelegate());
  }
}
