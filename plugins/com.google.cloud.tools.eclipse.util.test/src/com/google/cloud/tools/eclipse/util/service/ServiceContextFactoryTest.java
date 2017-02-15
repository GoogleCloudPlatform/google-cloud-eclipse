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

package com.google.cloud.tools.eclipse.util.service;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServiceContextFactoryTest {

  @Mock private IConfigurationElement configurationElement;

  @Test(expected = CoreException.class)
  public void testSetInitializationData_nullData() throws CoreException {
    new ServiceContextFactory().setInitializationData(configurationElement, "propertyName", null);
  }

  @Test(expected = CoreException.class)
  public void testSetInitializationData_nonStringData() throws CoreException {
    new ServiceContextFactory().setInitializationData(configurationElement, "propertyName", new Object());
  }

  @Test(expected = CoreException.class)
  public void testSetInitializationData_invalidBundleName() throws CoreException {
    when(configurationElement.getNamespaceIdentifier()).thenReturn("non.existent.bundle.name");
    new ServiceContextFactory().setInitializationData(configurationElement, "propertyName", new Object());
  }

  @Test(expected = CoreException.class)
  public void testSetInitializationData_invalidClassName() throws CoreException {
    when(configurationElement.getNamespaceIdentifier()).thenReturn("com.google.cloud,tools.eclipse.util");
    new ServiceContextFactory().setInitializationData(configurationElement, "propertyName", "com.example.Classname");
  }

  @Test
  public void testSetInitializationData_successful() throws CoreException {
    when(configurationElement.getNamespaceIdentifier()).thenReturn("com.google.cloud.tools.eclipse.util");
    new ServiceContextFactory().setInitializationData(configurationElement, "propertyName",
                                                      "com.google.cloud.tools.eclipse.util.service.TestInjectionTarget");
  }

  @Test
  public void testCreate() throws CoreException {
    when(configurationElement.getNamespaceIdentifier()).thenReturn("com.google.cloud.tools.eclipse.util");
    ServiceContextFactory factory = new ServiceContextFactory();
    factory.setInitializationData(configurationElement, "propertyName",
                                                      "com.google.cloud.tools.eclipse.util.service.TestInjectionTarget");
    Object testObject = factory.create();
    assertNotNull(testObject);
    assertThat(testObject, instanceOf(TestInjectionTarget.class));
    TestInjectionTarget testInjectionTarget = (TestInjectionTarget) testObject;
    assertNotNull(testInjectionTarget.getEclipseContext());
    assertNotNull(testInjectionTarget.getExtensionRegistry());
  }

}
