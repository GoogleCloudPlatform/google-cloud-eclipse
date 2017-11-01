/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.newproject.flex;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineProject;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProjectTest;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Test;

public class CreateAppEngineFlexWtpProjectTest extends CreateAppEngineWtpProjectTest {

  @Override
  protected CreateAppEngineProject newCreateAppEngineProject() {
    return new CreateAppEngineFlexWtpProject(config, mock(IAdaptable.class), repositoryService);
  }

  @Test
  public void testServletApi31Added() throws InvocationTargetException, CoreException {
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);

    assertTrue(project.getFile("lib/fake-javax.servlet-javax.servlet-api-3.1.0.jar").exists());
  }

  @Test
  public void testJspApi231Added() throws InvocationTargetException, CoreException {
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);

    assertTrue(project.getFile("lib/fake-javax.servlet.jsp-javax.servlet.jsp-api-2.3.1.jar")
        .exists());
  }

  @Test
  public void testNonDependencyAttributeOnJarsInLib()
      throws InvocationTargetException, CoreException {
    CreateAppEngineProject creator =
        new CreateAppEngineFlexWtpProject(config, mock(IAdaptable.class), repositoryService);
    creator.execute(monitor);

    File lib = project.getFolder("lib").getLocation().toFile();
    for (File jar : lib.listFiles()) {
      assertTrue(hasNonDependencyAttribute(jar));
    }
  }

  private boolean hasNonDependencyAttribute(File jar) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    for (IClasspathEntry entry : javaProject.getRawClasspath()) {
      if (entry.getPath().toFile().equals(jar)) {
        for (IClasspathAttribute attribute : entry.getExtraAttributes()) {
          if (isNonDependencyAttribute(attribute)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean isNonDependencyAttribute(IClasspathAttribute attribute) {
    return IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY
        .equals(attribute.getName());
  }

  @Test
  public void testDynamicWebModuleFacet31Added() throws InvocationTargetException, CoreException {
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertTrue(facetedProject.hasProjectFacet(WebFacetUtils.WEB_31));
  }

}
