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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour.PortProber;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalAppEngineServerBehaviourTest {

  private LocalAppEngineServerBehaviour serverBehavior = new LocalAppEngineServerBehaviour();
  @Mock private PortProber portProber;
  @Mock private IServer server;

  @Before
  public void setUp() {
    when(server.getAttribute(eq("appEngineDevServerAdminPort"), eq(8000))).thenReturn(8000);
    when(server.getAttribute(eq("appEngineDevServerAdminPort"), anyString())).thenReturn(null);
  }

  private void setUpServerPortAttribute(int serverPort) {
    when(server.getAttribute(eq("appEngineDevServerPort"), anyInt())).thenReturn(serverPort);
    when(server.getAttribute(eq("appEngineDevServerPort"), anyString()))
        .thenReturn(String.valueOf(serverPort));
  }

  private void setUpAdminPortAttribute(int adminPort) {
    when(server.getAttribute(eq("appEngineDevServerAdminPort"), anyInt())).thenReturn(adminPort);
    when(server.getAttribute(eq("appEngineDevServerAdminPort"), anyString()))
        .thenReturn(String.valueOf(adminPort));
  }

  @Test
  public void testCheckAndSetPorts() throws CoreException {
    setUpServerPortAttribute(65535);
    setUpAdminPortAttribute(8000);
    serverBehavior.checkAndSetPorts(server, portProber);

    assertEquals(65535, serverBehavior.getServerPort());
    assertEquals(8000, serverBehavior.adminPort);
  }

  @Test
  public void testCheckAndSetPorts_portZero() throws CoreException {
    setUpServerPortAttribute(0);
    setUpAdminPortAttribute(0);
    serverBehavior.checkAndSetPorts(server, portProber);

    assertEquals(0, serverBehavior.getServerPort());
    assertEquals(0, serverBehavior.adminPort);
  }

  @Test
  public void testCheckAndSetPorts_adminPortAttributeNotSet() throws CoreException {
    setUpServerPortAttribute(9800);
    serverBehavior.checkAndSetPorts(server, portProber);

    assertEquals(8000, serverBehavior.adminPort);
  }

  @Test
  public void testCheckAndSetPorts_negativeServerPort() {
    try {
      setUpServerPortAttribute(-1);
      setUpAdminPortAttribute(9000);
      serverBehavior.checkAndSetPorts(server, portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Port must be between 0 and 65535.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_negativeAdminPort() {
    try {
      setUpServerPortAttribute(9080);
      setUpAdminPortAttribute(-1);
      serverBehavior.checkAndSetPorts(server, portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Port must be between 0 and 65535.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_outOfBoundServerPort() {
    try {
      setUpServerPortAttribute(65536);
      setUpAdminPortAttribute(9000);
      serverBehavior.checkAndSetPorts(server, portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Port must be between 0 and 65535.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_outOfBoundAdminPort() {
    try {
      setUpServerPortAttribute(9080);
      setUpAdminPortAttribute(65536);
      serverBehavior.checkAndSetPorts(server, portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Port must be between 0 and 65535.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_serverPortInUse() {
    try {
      when(portProber.isPortInUse(65535)).thenReturn(true);
      setUpServerPortAttribute(65535);
      setUpAdminPortAttribute(9000);
      serverBehavior.checkAndSetPorts(server, portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Port 65535 is in use.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_adminPortNotSetAndPortInuse() throws CoreException {
    when(portProber.isPortInUse(8000)).thenReturn(true);
    setUpServerPortAttribute(9080);
    serverBehavior.checkAndSetPorts(server, portProber);
    assertEquals(9080, serverBehavior.getServerPort());
    assertEquals(0, serverBehavior.adminPort);
  }

  @Test
  public void testCheckAndSetPorts_adminPortSetAndPortInUse() {
    try {
      when(portProber.isPortInUse(65535)).thenReturn(true);
      setUpServerPortAttribute(9080);
      setUpAdminPortAttribute(65535);
      serverBehavior.checkAndSetPorts(server, portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Port 65535 is in use.", ex.getMessage());
    }
  }

  private static final String[] serverOutputWithDefaultModule1 = new String[] {
      "WARNING  2016-11-03 21:11:21,930 devappserver2.py:785] DEFAULT_VERSION_HOSTNAME will not be set correctly with --port=0",
      "INFO     2016-11-03 21:11:21,956 api_server.py:205] Starting API server at: http://localhost:52892",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"default\" running at: http://localhost:55948",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"second\" running at: http://localhost:8081",
      "INFO     2016-11-03 21:11:21,959 admin_server.py:116] Starting admin server at: http://localhost:43679",
      "Nov 03, 2016 9:11:23 PM com.google.appengine.tools.development.SystemPropertiesManager setSystemProperties"
  };

  private static final String[] serverOutputWithDefaultModule2 = new String[] {
      "WARNING  2016-11-03 21:11:21,930 devappserver2.py:785] DEFAULT_VERSION_HOSTNAME will not be set correctly with --port=0",
      "INFO     2016-11-03 21:11:21,956 api_server.py:205] Starting API server at: http://localhost:52892",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"first\" running at: http://localhost:55948",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"default\" running at: http://localhost:8081",
      "INFO     2016-11-03 21:11:21,959 admin_server.py:116] Starting admin server at: http://localhost:43679",
      "Nov 03, 2016 9:11:23 PM com.google.appengine.tools.development.SystemPropertiesManager setSystemProperties"
  };

  private static final String[] serverOutputWithNoDefaultModule = new String[] {
      "WARNING  2016-11-03 21:11:21,930 devappserver2.py:785] DEFAULT_VERSION_HOSTNAME will not be set correctly with --port=0",
      "INFO     2016-11-03 21:11:21,956 api_server.py:205] Starting API server at: http://localhost:52892",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"first\" running at: http://localhost:8181",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"second\" running at: http://localhost:8182",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"third\" running at: http://localhost:8183",
      "INFO     2016-11-03 21:11:21,959 admin_server.py:116] Starting admin server at: http://localhost:43679",
      "Nov 03, 2016 9:11:23 PM com.google.appengine.tools.development.SystemPropertiesManager setSystemProperties"
  };

  @Test
  public void testExtractServerPortFromOutput_firstModuleIsDefault() throws CoreException {
    setUpServerPortAttribute(0);
    serverBehavior.checkAndSetPorts(server, portProber);

    simulateOutputParsing(serverOutputWithDefaultModule1);
    assertEquals(55948, serverBehavior.getServerPort());
  }

  @Test
  public void testExtractServerPortFromOutput_secondModuleIsDefault() throws CoreException {
    setUpServerPortAttribute(0);
    serverBehavior.checkAndSetPorts(server, portProber);

    simulateOutputParsing(serverOutputWithDefaultModule2);
    assertEquals(8081, serverBehavior.getServerPort());
  }

  @Test
  public void testExtractServerPortFromOutput_noDefaultModule() throws CoreException {
    setUpServerPortAttribute(0);
    serverBehavior.checkAndSetPorts(server, portProber);

    simulateOutputParsing(serverOutputWithNoDefaultModule);
    assertEquals(8181, serverBehavior.getServerPort());
  }

  @Test
  public void testExtractServerPortFromOutput_defaultModuleDoesNotOverrideUserSpecifiedPort()
      throws CoreException {
    setUpServerPortAttribute(12345);
    serverBehavior.checkAndSetPorts(server, portProber);

    simulateOutputParsing(serverOutputWithDefaultModule1);
    assertEquals(12345, serverBehavior.getServerPort());
  }

  @Test
  public void testExtractModuleUrlFromOutput_firstModuleIsDefault() throws CoreException {
    serverBehavior.checkAndSetPorts(server, portProber);
    simulateOutputParsing(serverOutputWithDefaultModule1);
    assertEquals("http://localhost:55948", serverBehavior.getServiceUrl("default"));
    assertEquals("http://localhost:8081", serverBehavior.getServiceUrl("second"));
  }

  @Test
  public void testExtractModuleUrlFromOutput_noDefaultModule() throws CoreException {
    serverBehavior.checkAndSetPorts(server, portProber);
    simulateOutputParsing(serverOutputWithNoDefaultModule);
    assertNull(serverBehavior.getServiceUrl("default"));
    assertEquals("http://localhost:8181", serverBehavior.getServiceUrl("first"));
    assertEquals("http://localhost:8182", serverBehavior.getServiceUrl("second"));
    assertEquals("http://localhost:8183", serverBehavior.getServiceUrl("third"));
  }

  @Test
  public void testExtractAdminPortFromOutput() throws CoreException {
    when(portProber.isPortInUse(8000)).thenReturn(true);
    setUpServerPortAttribute(9080);
    setUpAdminPortAttribute(0);
    serverBehavior.checkAndSetPorts(server, portProber);

    simulateOutputParsing(serverOutputWithDefaultModule1);
    assertEquals(43679, serverBehavior.adminPort);
  }

  private void simulateOutputParsing(String[] output) {
    LocalAppEngineServerBehaviour.DevAppServerOutputListener outputListener =
        serverBehavior.new DevAppServerOutputListener();
    for (String line : output) {
      outputListener.onOutputLine(line);
    }
  }
}
