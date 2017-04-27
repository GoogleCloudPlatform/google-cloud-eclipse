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
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.cloud.tools.eclipse.appengine.facets.convert.AppEngineStandardProjectConvertJob;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that our <em>Convert to App Engine Standard Project</em> doesn't downgrade Java or jst.web
 * facet versions.
 */
public class ConversionTests {
  @Rule
  public TestProjectCreator testProject =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31);

  @Test
  public void testChangeToAndFrom_1_8() throws CoreException, IOException, InterruptedException {
    IFacetedProject project = testProject.getFacetedProject();
    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();

    // ensure facet versions haven't been downgraded
    assertEquals(JavaFacet.VERSION_1_8, project.getProjectFacetVersion(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_31, project.getProjectFacetVersion(WebFacetUtils.WEB_FACET));
    // ensure appengine-web.xml has <runtime>java8</runtime>
    IFile appengineWebXml =
        WebProjectUtil.findInWebInf(project.getProject(), new Path("appengine-web.xml"));
    assertTrue(appengineWebXml.exists());
    try (InputStream input = appengineWebXml.getContents()) {
      assertTrue(AppEngineDescriptor.parse(input).isJava8());
    }
  }
}
