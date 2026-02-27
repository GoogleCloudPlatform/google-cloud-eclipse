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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.operations.cloudsdk.process.ProcessOutputLineListener;
import java.net.InetAddress;
import java.util.function.BiPredicate;
import org.eclipse.core.runtime.CoreException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalAppEngineServerBehaviourTest {
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock private BiPredicate<InetAddress, Integer> alwaysTrue;
  @Mock private BiPredicate<InetAddress, Integer> alwaysFalse;
  @Mock private BiPredicate<InetAddress, Integer> portProber;

  private final LocalAppEngineServerBehaviour serverBehavior = new LocalAppEngineServerBehaviour();

  @Before
  public void setUp() {
    when(alwaysTrue.test(any(InetAddress.class), anyInt())).thenReturn(true);
    when(alwaysFalse.test(any(InetAddress.class), anyInt())).thenReturn(false);
  }

  @Test
  public void testCheckPort_port0() throws CoreException {
    // port 0 should never be checked if in use
    assertEquals(0, LocalAppEngineServerBehaviour.checkPort(null, 0, portProber));
    verify(portProber, never()).test(any(InetAddress.class), any(Integer.class));
  }

  @Test
  public void testCheckPort_portNotInUse() throws CoreException {
    assertEquals(1, LocalAppEngineServerBehaviour.checkPort(null, 1, alwaysFalse));
    verify(alwaysFalse).test(any(InetAddress.class), any(Integer.class));
  }

  @Test
  public void testCheckPort_portInUse() {
    try {
      LocalAppEngineServerBehaviour.checkPort(null, 1, alwaysTrue);
      fail("Should throw CoreException");
    } catch (CoreException ex) {
      assertEquals("Port 1 is in use.", ex.getMessage());
      verify(alwaysTrue).test(any(InetAddress.class), any(Integer.class));
    }
  }

  @Test
  public void testCheckPort_portOutOfBounds_negative() {
    try {
      LocalAppEngineServerBehaviour.checkPort(null, -1, portProber);
      fail("Should throw CoreException");
    } catch (CoreException ex) {
      assertEquals("Port must be between 0 and 65535.", ex.getMessage());
      verify(portProber, never()).test(any(InetAddress.class), any(Integer.class));
    }
  }

  @Test
  public void testCheckPort_portOutOfBounds_positive() {
    try {
      LocalAppEngineServerBehaviour.checkPort(null, 65536, portProber);
      fail("Should throw CoreException");
    } catch (CoreException ex) {
      assertEquals("Port must be between 0 and 65535.", ex.getMessage());
      verify(portProber, never()).test(any(InetAddress.class), any(Integer.class));
    }
  }

  private static final String[] devappserver1Output = new String[] {
      "2019-03-01 17:14:16.279:INFO:oejs.Server:main: jetty-9.4.14.v20181114; built: 2018-11-14T21:20:31.478Z; git: c4550056e785fb5665914545889f21dc136ad9e6; jvm 1.8.0_161-b12",
      "Mar 01, 2019 10:14:16 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: Module instance alice is running at http://localhost:7979/",
      "Mar 01, 2019 10:14:16 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: The admin console is running at http://localhost:7979/_ah/admin"
  };

  private static final String[] devappserver1OutputWithDefaultModule1 = new String[] {
      "2019-03-01 17:01:53.026:INFO:oejs.Server:main: jetty-9.4.14.v20181114; built: 2018-11-14T21:20:31.478Z; git: c4550056e785fb5665914545889f21dc136ad9e6; jvm 1.8.0_161-b12",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: Module instance default is running at http://localhost:55948/",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: The admin console is running at http://localhost:55948/_ah/admin",
      "2019-03-01 17:01:53.392:INFO:oejs.Server:main: jetty-9.4.14.v20181114; built: 2018-11-14T21:20:31.478Z; git: c4550056e785fb5665914545889f21dc136ad9e6; jvm 1.8.0_161-b12",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: Module instance second is running at http://localhost:8081/",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: The admin console is running at http://localhost:8081/_ah/admin"
  };

  private static final String[] devappserver1OutputWithDefaultModule2 = new String[] {
      "2019-03-01 17:01:53.026:INFO:oejs.Server:main: jetty-9.4.14.v20181114; built: 2018-11-14T21:20:31.478Z; git: c4550056e785fb5665914545889f21dc136ad9e6; jvm 1.8.0_161-b12",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: Module instance first is running at http://localhost:55948/",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: The admin console is running at http://localhost:55948/_ah/admin",
      "2019-03-01 17:01:53.392:INFO:oejs.Server:main: jetty-9.4.14.v20181114; built: 2018-11-14T21:20:31.478Z; git: c4550056e785fb5665914545889f21dc136ad9e6; jvm 1.8.0_161-b12",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: Module instance default is running at http://localhost:8081/",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: The admin console is running at http://localhost:8081/_ah/admin"
  };

  private static final String[] devappserver1OutputWithNoDefaultModule = new String[] {
      "2019-03-01 17:01:53.026:INFO:oejs.Server:main: jetty-9.4.14.v20181114; built: 2018-11-14T21:20:31.478Z; git: c4550056e785fb5665914545889f21dc136ad9e6; jvm 1.8.0_161-b12",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: Module instance first is running at http://localhost:8181/",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: The admin console is running at http://localhost:8181/_ah/admin",
      "2019-03-01 17:01:53.392:INFO:oejs.Server:main: jetty-9.4.14.v20181114; built: 2018-11-14T21:20:31.478Z; git: c4550056e785fb5665914545889f21dc136ad9e6; jvm 1.8.0_161-b12",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: Module instance second is running at http://localhost:8182/",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: The admin console is running at http://localhost:8182/_ah/admin",
      "2019-03-01 17:01:53.482:INFO:oejs.Server:main: jetty-9.4.14.v20181114; built: 2018-11-14T21:20:31.478Z; git: c4550056e785fb5665914545889f21dc136ad9e6; jvm 1.8.0_161-b12",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: Module instance third is running at http://localhost:8183/",
      "Mar 01, 2019 10:01:53 PM com.google.appengine.tools.development.AbstractModule startup",
      "INFO: The admin console is running at http://localhost:8183/_ah/admin"
  };
  
  @Test
  public void testExtractServerPortFromOutput_devappserver1() {
    setUpServerPort(0);
    simulateOutputParsing(devappserver1Output);
    assertEquals(7979, serverBehavior.getServerPort());
  }

  @Test
  public void testExtractServerPortFromOutput_firstModuleIsDefault() {
    setUpServerPort(0);
    simulateOutputParsing(devappserver1OutputWithDefaultModule1);
    assertEquals(55948, serverBehavior.getServerPort());
  }

  @Test
  public void testExtractServerPortFromOutput_secondModuleIsDefault() {
    setUpServerPort(0);
    simulateOutputParsing(devappserver1OutputWithDefaultModule2);
    assertEquals(8081, serverBehavior.getServerPort());
  }

  @Test
  public void testExtractServerPortFromOutput_noDefaultModule() {
    setUpServerPort(0);
    simulateOutputParsing(devappserver1OutputWithNoDefaultModule);
    assertEquals(8181, serverBehavior.getServerPort());
  }

  @Test
  public void testExtractServerPortFromOutput_defaultModuleDoesNotOverrideUserSpecifiedPort() {
    setUpServerPort(12345);
    simulateOutputParsing(devappserver1OutputWithDefaultModule1);
    assertEquals(12345, serverBehavior.getServerPort());
  }

  @Test
  public void testExtractModuleUrlFromOutput_firstModuleIsDefault() {
    simulateOutputParsing(devappserver1OutputWithDefaultModule1);
    assertEquals("http://localhost:55948/", serverBehavior.getServiceUrl("default"));
    assertEquals("http://localhost:8081/", serverBehavior.getServiceUrl("second"));
  }

  @Test
  public void testExtractModuleUrlFromOutput_noDefaultModule() {
    simulateOutputParsing(devappserver1OutputWithNoDefaultModule);
    assertNull(serverBehavior.getServiceUrl("default"));
    assertEquals("http://localhost:8181/", serverBehavior.getServiceUrl("first"));
    assertEquals("http://localhost:8182/", serverBehavior.getServiceUrl("second"));
    assertEquals("http://localhost:8183/", serverBehavior.getServiceUrl("third"));
  }

  private void setUpServerPort(int port) {
    serverBehavior.serverPort = port;
  }

  private void simulateOutputParsing(String[] output) {
    ProcessOutputLineListener outputListener = serverBehavior.new DevAppServerOutputListener();
    for (String line : output) {
      outputListener.onOutputLine(line);
    }
  }
}
