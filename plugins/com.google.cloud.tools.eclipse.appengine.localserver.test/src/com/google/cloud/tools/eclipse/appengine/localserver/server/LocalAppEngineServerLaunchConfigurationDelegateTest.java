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

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalAppEngineServerLaunchConfigurationDelegateTest {

  @Mock private IServer server;
  @Mock private LocalAppEngineServerBehaviour serverBehavior;

  @Before
  public void setUp() {
    when(server.loadAdapter(any(Class.class), any(IProgressMonitor.class)))
        .thenReturn(serverBehavior);
  }

  @Test
  public void testDeterminePageLocation() {
    when(server.getHost()).thenReturn("192.168.1.1");
    when(serverBehavior.getPort()).thenReturn(8085);

    String url = LocalAppEngineServerLaunchConfigurationDelegate.determinePageLocation(server);
    assertEquals("http://192.168.1.1:8085", url);
  }
}
