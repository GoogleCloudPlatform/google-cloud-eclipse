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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test that the URL hyperlink pattern matches our anticipated URLs
 */
public class UrlLinkTest {
  // console text with embedded URLs: each line has the associated URL or null
  private final String[][] consoleTexts =
      {{"INFO     2016-08-19 19:49:38,095 devappserver2.py:769] Skipping SDK update check.", null},
          {"INFO     2016-08-19 19:49:38,161 api_server.py:205] Starting API server at: http://localhost:60320",
              "http://localhost:60320"},
          {"INFO     2016-08-19 19:49:38,166 dispatcher.py:197] Starting module \"default\" running at: http://localhost:8080",
              "http://localhost:8080"},
          {"INFO     2016-08-19 19:49:38,172 admin_server.py:116] Starting admin server at: http://localhost:8000",
              "http://localhost:8000"},
          {"Aug 19, 2016 7:49:39 PM com.google.appengine.tools.development.AbstractContainerService configure",
              null},
          {"WARNING: Null value for containerConfigProperties.get(devappserver.portMappingProvider)",
              null},
          {"Aug 19, 2016 7:49:39 PM com.google.apphosting.utils.jetty.JettyLogger info", null},
          {"INFO: Logging to JettyLogger(null) via com.google.apphosting.utils.jetty.JettyLogger",
              null},
          {"Aug 19, 2016 7:49:39 PM com.google.apphosting.utils.jetty.JettyLogger info", null},
          {"INFO: jetty-6.1.x", null},
          {"Aug 19, 2016 7:49:40 PM org.apache.jasper.EmbeddedServletOptions <init>", null},
          {"WARNING: Warning: Invalid value for the initParam keepgenerated. Will use the default value of \"false\"",
              null},
          {"Aug 19, 2016 7:49:40 PM org.apache.jasper.EmbeddedServletOptions <init>", null},
          {"WARNING: Warning: Invalid value for the initParam fork. Will use the default value of \"true\"",
              null},
          {"Aug 19, 2016 7:49:40 PM com.google.apphosting.utils.jetty.JettyLogger info", null},
          {"INFO: Started SelectChannelConnector@localhost:60329", null},
          {"Aug 19, 2016 7:49:40 PM com.google.appengine.tools.development.AbstractModule startup",
              null},
          {"INFO: Module instance default is running at http://localhost:60329/",
              "http://localhost:60329/"},
          {"Aug 19, 2016 7:49:40 PM com.google.appengine.tools.development.AbstractModule startup",
              null},
          {"INFO: The admin console is running at http://localhost:60329/_ah/admin",
              "http://localhost:60329/_ah/admin"},
          {"Aug 19, 2016 7:49:40 PM com.google.appengine.tools.development.devappserver2.DevAppServer2Impl doStart",
              null},
          {"INFO: Dev App Server is now running", null},
          {"INFO     2016-08-19 19:49:40,913 module.py:788] default: \"GET / HTTP/1.1\" 200 582",
              null}};

  private Pattern regexp;

  @Before
  public void setUp() {
    IExtensionRegistry registry = RegistryFactory.getRegistry();
    IExtension extension =
        registry.getExtension("org.eclipse.ui.console.consolePatternMatchListeners",
            "com.google.cloud.tools.eclipse.appengine.localserver.urlLinker");
    assertNotNull("URL linking extension not found", extension);
    assertEquals("Should only have a single URL linker", 1,
        extension.getConfigurationElements().length);
    IConfigurationElement definition = extension.getConfigurationElements()[0];
    assertEquals("consolePatternMatchListener", definition.getName());
    assertNotNull(definition.getAttribute("regex"));

    regexp = Pattern.compile(".*(" + definition.getAttribute("regex") + ").*");
  }

  @Test
  public void testMatches() {
    for (String[] candidate : consoleTexts) {
      Matcher m = regexp.matcher(candidate[0]);
      if (candidate[1] == null) {
        assertFalse(m.matches());
      } else {
        assertTrue(m.matches());
        assertEquals(candidate[1], m.group(1));
      }
    }
  }

  @Test
  public void testHttps() {
    Matcher m = regexp.matcher("does this match https://localhost:8080/ and not the remainder");
    assertTrue(m.matches());
    assertEquals("https://localhost:8080/", m.group(1));
  }
}
