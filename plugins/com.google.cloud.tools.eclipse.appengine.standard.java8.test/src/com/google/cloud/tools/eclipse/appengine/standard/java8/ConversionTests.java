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

package com.google.cloud.tools.eclipse.appengine.standard.java8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineConfigurationUtil;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.convert.AppEngineStandardProjectConvertJob;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import com.google.common.io.CharStreams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Test that our <em>Convert to App Engine Standard Project</em> doesn't downgrade Java or jst.web
 * facet versions.
 */
public class ConversionTests {
  @Rule
  public TestProjectCreator projectCreator = new TestProjectCreator();

  private String originalAppEngineWebContent;

  @Test
  public void bare_Java7_Web25()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator
        .withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25).getFacetedProject();

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    // ensure facet versions haven't been downgraded
    assertFacetVersions(project, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
        AppEngineStandardFacet.JRE7);
    assertNoJava8Runtime(project);
  }

  @Test
  public void bare_Java7_Web31()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator
        .withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_31).getFacetedProject();
    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    // ensure that java facet upgraded, and web facet versions not downgraded
    assertFacetVersions(project, JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31,
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);
    assertJava8Runtime(project);
  }

  @Test
  public void bare_Java8_Web25()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator
        .withFacets(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25).getFacetedProject();
    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    // ensure facet versions haven't been downgraded
    assertFacetVersions(project, JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25,
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);
    assertJava8Runtime(project);
  }

  @Test
  public void bare_Java8_Web31()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator
        .withFacets(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31).getFacetedProject();
    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    // ensure facet versions haven't been downgraded
    assertFacetVersions(project, JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31,
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);
    assertJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithNoRuntime()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator.getFacetedProject();
    createAppEngineWebWithNoRuntime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
        AppEngineStandardFacet.JRE7);
    assertNoJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithNoRuntime_Java7()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project =
        projectCreator.withFacets(JavaFacet.VERSION_1_7).getFacetedProject();
    createAppEngineWebWithNoRuntime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
        AppEngineStandardFacet.JRE7);
    assertNoJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithNoRuntime_Java7_Web25()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator
        .withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25).getFacetedProject();
    createAppEngineWebWithNoRuntime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
        AppEngineStandardFacet.JRE7);
    assertNoJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithNoRuntime_Java7_Web31()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator
        .withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_31).getFacetedProject();
    createAppEngineWebWithNoRuntime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
        AppEngineStandardFacet.JRE7);
    assertNoJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithNoRuntime_Java8()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project =
        projectCreator.withFacets(JavaFacet.VERSION_1_8).getFacetedProject();
    createAppEngineWebWithNoRuntime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
        AppEngineStandardFacet.JRE7);
    assertNoJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithNoRuntime_Java8_Web25()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator
        .withFacets(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25).getFacetedProject();
    createAppEngineWebWithNoRuntime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
        AppEngineStandardFacet.JRE7);
    assertNoJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithNoRuntime_Java8_Web31()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator
        .withFacets(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31).getFacetedProject();
    createAppEngineWebWithNoRuntime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
        AppEngineStandardFacet.JRE7);
    assertNoJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithJava8Runtime()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator.getFacetedProject();
    createAppEngineWebWithJava8Runtime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31,
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);
    assertJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithJava8Runtime_Java7()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project =
        projectCreator.withFacets(JavaFacet.VERSION_1_7).getFacetedProject();
    createAppEngineWebWithJava8Runtime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31,
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);
    assertJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithJava8Runtime_Java7_Web25()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator
        .withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25).getFacetedProject();
    createAppEngineWebWithJava8Runtime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25,
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);
    assertJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithJava8Runtime_Java7_Web31()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator
        .withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_31).getFacetedProject();
    createAppEngineWebWithJava8Runtime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31,
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);
    assertJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithJava8Runtime_Java8()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project =
        projectCreator.withFacets(JavaFacet.VERSION_1_8).getFacetedProject();
    createAppEngineWebWithJava8Runtime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31,
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);
    assertJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithJava8Runtime_Java8_Web25()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator
        .withFacets(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25).getFacetedProject();
    createAppEngineWebWithJava8Runtime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25,
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);
    assertJava8Runtime(project);
  }

  @Test
  public void appEngineWebWithJava8Runtime_Java8_Web31()
      throws CoreException, IOException, InterruptedException, SAXException, AppEngineException {
    IFacetedProject project = projectCreator
        .withFacets(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31).getFacetedProject();
    createAppEngineWebWithJava8Runtime(project);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();
    assertIsOk("conversion should never fail", conversionJob.getResult());

    assertFacetVersions(project, JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31,
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);
    assertJava8Runtime(project);
  }


  /*******************************************************************/

  private void createAppEngineWebWithNoRuntime(IFacetedProject project) throws CoreException {
    IFolder webInf = project.getProject().getFolder("WebContent/WEB-INF");
    ResourceUtils.createFolders(webInf, null);
    IFile appEngineWeb = webInf.getFile("appengine-web.xml");
    originalAppEngineWebContent =
        "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'></appengine-web-app>";
    appEngineWeb.create(
        new ByteArrayInputStream(originalAppEngineWebContent.getBytes(StandardCharsets.UTF_8)),
        true, null);
  }

  private void createAppEngineWebWithJava8Runtime(IFacetedProject project) throws CoreException {
    IFolder webInf = project.getProject().getFolder("WebContent/WEB-INF");
    ResourceUtils.createFolders(webInf, null);
    IFile appengineWebXml = webInf.getFile("appengine-web.xml");
    originalAppEngineWebContent =
        "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'><runtime>java8</runtime></appengine-web-app>";
    appengineWebXml.create(
        new ByteArrayInputStream(originalAppEngineWebContent.getBytes(StandardCharsets.UTF_8)),
        true, null);
  }

  /** Verify that appengine-web.xml has <runtime>java8</runtime>. */
  private void assertJava8Runtime(IFacetedProject project)
      throws IOException, SAXException, CoreException, AppEngineException {
    IFile appengineWebXml =
        AppEngineConfigurationUtil.findConfigurationFile(
            project.getProject(), new Path("appengine-web.xml"));
    assertNotNull("appengine-web.xml is missing", appengineWebXml);
    assertTrue("appengine-web.xml does not exist", appengineWebXml.exists());
    try (InputStream contents = appengineWebXml.getContents()) {
      String appEngineWebContent =
          CharStreams.toString(new InputStreamReader(contents, StandardCharsets.UTF_8));
      if (originalAppEngineWebContent != null) {
        assertEquals("appengine-web.xml was changed", originalAppEngineWebContent,
            appEngineWebContent);
      }
      AppEngineDescriptor descriptor = AppEngineDescriptor
          .parse(new ByteArrayInputStream(appEngineWebContent.getBytes(StandardCharsets.UTF_8)));
      assertTrue("should have <runtime>java8</runtime>", descriptor.isJava8());
    }
  }

  /** Verify that appengine-web.xml has no <runtime>java8</runtime>. */
  private void assertNoJava8Runtime(IFacetedProject project)
      throws IOException, SAXException, CoreException, AppEngineException {
    IFile appengineWebXml =
        AppEngineConfigurationUtil.findConfigurationFile(
            project.getProject(), new Path("appengine-web.xml"));
    assertNotNull("appengine-web.xml is missing", appengineWebXml);
    assertTrue("appengine-web.xml does not exist", appengineWebXml.exists());
    try (InputStream contents = appengineWebXml.getContents()) {
      String appEngineWebContent =
          CharStreams.toString(new InputStreamReader(contents, StandardCharsets.UTF_8));
      if (originalAppEngineWebContent != null) {
        assertEquals("appengine-web.xml was changed", originalAppEngineWebContent,
            appEngineWebContent);
      }
      AppEngineDescriptor descriptor = AppEngineDescriptor
          .parse(new ByteArrayInputStream(appEngineWebContent.getBytes(StandardCharsets.UTF_8)));
      assertFalse("should not have <runtime>java8</runtime>", descriptor.isJava8());
      assertEquals("should report runtime=java7", "java7", descriptor.getRuntime());
    }
  }

  private void assertFacetVersions(IFacetedProject project, IProjectFacetVersion... versions) {
    for (IProjectFacetVersion version : versions) {
      assertEquals(version, project.getProjectFacetVersion(version.getProjectFacet()));
    }
  }

  private void assertIsOk(String message, IStatus status) {
    assertTrue(message + ": " + status, status.isOK());
  }
}
