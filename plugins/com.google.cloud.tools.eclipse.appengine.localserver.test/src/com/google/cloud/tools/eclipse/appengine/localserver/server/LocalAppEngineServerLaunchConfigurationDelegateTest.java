/*******************************************************************************
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
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.wst.server.core.IServer;
import org.junit.Test;

public class LocalAppEngineServerLaunchConfigurationDelegateTest {

  @Test
  public void testDeterminePageLocation() {
    IServer server = mock(IServer.class);
    when(server.getHost()).thenReturn("192.168.1.1");
    when(server.getAttribute(eq("appEngineDevServerPort"), anyInt())).thenReturn(8085);

    String url =
        LocalAppEngineServerLaunchConfigurationDelegate.determinePageLocation(server, 8085);
    assertEquals("http://192.168.1.1:8085", url);
  }

  @Test
  public void testDeterminePageLocation_actualPortMayDifferFromConfigPort() {
    IServer server = mock(IServer.class);
    when(server.getHost()).thenReturn("192.168.100.1");
    when(server.getAttribute(eq("appEngineDevServerPort"), anyInt())).thenReturn(0);

    String url =
        LocalAppEngineServerLaunchConfigurationDelegate.determinePageLocation(server, 12345);
    assertEquals("http://192.168.100.1:12345", url);
  }
}
