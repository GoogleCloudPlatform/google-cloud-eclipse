/*
 * Copyright 2018 Google LLC
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

package com.google.cloud.tools.eclipse.util.jdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JreDetectorTest {
  /** Where our JRE/JDK-like installations are created. */
  @Rule public TemporaryFolder rootLocation = new TemporaryFolder();

  private IExecutionEnvironmentsManager manager;

  // not actually linked with any VMs
  private IExecutionEnvironment javase7;
  private IExecutionEnvironment javase8;
  private IExecutionEnvironment javase9;
  private IExecutionEnvironment javase10;

  private IVMInstall jre7;
  private IVMInstall jdk7;
  private IVMInstall jre8;
  private IVMInstall jdk8;
  private IVMInstall jre9;
  private IVMInstall jdk9;
  private IVMInstall jre10;
  private IVMInstall jdk10;

  @Before
  public void setUp() throws IOException {
    javase7 = mock(IExecutionEnvironment.class, "JavaSE-1.7");
    when(javase7.getId()).thenReturn("JavaSE-1.7");
    javase8 = mock(IExecutionEnvironment.class, "JavaSE-1.8");
    when(javase8.getId()).thenReturn("JavaSE-1.8");
    javase9 = mock(IExecutionEnvironment.class, "JavaSE-9");
    when(javase9.getId()).thenReturn("JavaSE-9");
    javase10 = mock(IExecutionEnvironment.class, "JavaSE-10");
    when(javase10.getId()).thenReturn("JavaSE-10");

    manager = mock(IExecutionEnvironmentsManager.class);
    Map<String, IExecutionEnvironment> environments = new HashMap<>();
    environments.put(javase7.getId(), javase7);
    environments.put(javase8.getId(), javase8);
    environments.put(javase9.getId(), javase9);
    environments.put(javase10.getId(), javase10);
    when(manager.getEnvironment(anyString()))
        .thenAnswer(invocation -> environments.get(invocation.getArgumentAt(0, String.class)));

    jre7 = mockOldJre("JRE7");
    jdk7 = mockOldJdk("JDK7");
    jre8 = mockOldJre("JRE8");
    jdk8 = mockOldJdk("JDK8");
    jre9 = mockModuleJre("JRE9");
    jdk9 = mockModuleJdk("JDK9");
    jre10 = mockModuleJre("JRE10");
    jdk10 = mockModuleJdk("JDK10");

    when(javase7.getDefaultVM()).thenReturn(jre7);
    when(javase7.getCompatibleVMs())
        .thenReturn(new IVMInstall[] {jre7, jdk7, jre8, jdk8, jre9, jdk9, jre10, jdk10});
    when(javase8.getDefaultVM()).thenReturn(jre8);
    when(javase8.getCompatibleVMs())
        .thenReturn(new IVMInstall[] {jre8, jdk8, jre9, jdk9, jre10, jdk10});
    when(javase9.getDefaultVM()).thenReturn(jre9);
    when(javase9.getCompatibleVMs()).thenReturn(new IVMInstall[] {jre9, jdk9, jre10, jdk10});
    when(javase10.getDefaultVM()).thenReturn(jre10);
    when(javase10.getCompatibleVMs()).thenReturn(new IVMInstall[] {jre10, jdk10});
  }

  /** Mock up an old-style JRE. The provided ID is just for debugging purposes. */
  private IVMInstall mockOldJre(String id) throws IOException {
    File location = rootLocation.newFolder(id);
    File lib = new File(location, "lib");
    assertTrue(lib.mkdir());
    IVMInstall install = mock(IVMInstall.class);
    when(install.getId()).thenReturn(id);
    when(install.getInstallLocation()).thenReturn(location);
    return install;
  }

  /** Mock up an old-style JDK. The provided ID is just for debugging purposes. */
  private IVMInstall mockOldJdk(String id) throws IOException {
    File location = rootLocation.newFolder(id);
    File lib = new File(location, "lib");
    assertTrue(lib.mkdir());
    File toolsJar = new File(lib, "tools.jar");
    assertTrue(toolsJar.createNewFile());
    IVMInstall install = mock(IVMInstall.class);
    when(install.getId()).thenReturn(id);
    when(install.getInstallLocation()).thenReturn(location);
    return install;
  }

  /** Mock up an new-style JRE with modules. The provided ID is just for debugging purposes. */
  private IVMInstall mockModuleJre(String id) throws IOException {
    File location = rootLocation.newFolder(id);
    File jmods = new File(location, "jmods");
    assertTrue(jmods.mkdir());
    IVMInstall install = mock(IVMInstall.class);
    when(install.getId()).thenReturn(id);
    when(install.getInstallLocation()).thenReturn(location);
    return install;
  }

  /** Mock up an new-style JDK with modules. The provided ID is just for debugging purposes. */
  private IVMInstall mockModuleJdk(String id) throws IOException {
    File location = rootLocation.newFolder(id);
    File jmods = new File(location, "jmods");
    assertTrue(jmods.mkdir());
    assertTrue(new File(jmods, "java.compiler.jmod").createNewFile());
    IVMInstall install = mock(IVMInstall.class);
    when(install.getId()).thenReturn(id);
    when(install.getInstallLocation()).thenReturn(location);
    return install;
  }

  @Test
  public void testDetermineExecutionEnvironment() {
    assertEquals("JavaSE-1.7", JreDetector.determineExecutionEnvironment(manager, jre7));
    assertEquals("JavaSE-1.7", JreDetector.determineExecutionEnvironment(manager, jdk7));
    assertEquals("JavaSE-1.8", JreDetector.determineExecutionEnvironment(manager, jre8));
    assertEquals("JavaSE-1.8", JreDetector.determineExecutionEnvironment(manager, jdk8));
    assertEquals("JavaSE-9", JreDetector.determineExecutionEnvironment(manager, jre9));
    assertEquals("JavaSE-9", JreDetector.determineExecutionEnvironment(manager, jdk9));
    assertEquals("JavaSE-10", JreDetector.determineExecutionEnvironment(manager, jre10));
    assertEquals("JavaSE-10", JreDetector.determineExecutionEnvironment(manager, jdk10));

    assertEquals(
        "UNKNOWN",
        JreDetector.determineExecutionEnvironment(manager, mock(IVMInstall.class, "UNKNOWN")));
  }

  @Test
  public void testIsDevelopmentKit() {
    assertFalse(JreDetector.isDevelopmentKit(manager, jre7));
    assertTrue(JreDetector.isDevelopmentKit(manager, jdk7));
    assertFalse(JreDetector.isDevelopmentKit(manager, jre8));
    assertTrue(JreDetector.isDevelopmentKit(manager, jdk8));
    assertFalse(JreDetector.isDevelopmentKit(manager, jre9));
    assertTrue(JreDetector.isDevelopmentKit(manager, jdk9));
    assertFalse(JreDetector.isDevelopmentKit(manager, jre10));
    assertTrue(JreDetector.isDevelopmentKit(manager, jdk10));

    assertFalse(JreDetector.isDevelopmentKit(manager, mock(IVMInstall.class, "UNKNOWN")));
  }
}
