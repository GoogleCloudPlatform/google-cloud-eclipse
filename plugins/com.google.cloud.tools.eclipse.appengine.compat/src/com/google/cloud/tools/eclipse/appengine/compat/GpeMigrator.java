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

package com.google.cloud.tools.eclipse.appengine.compat;

import com.google.cloud.tools.eclipse.util.NatureUtils;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

public class GpeMigrator {

  private static final String GPE_GAE_NATURE_ID = "com.google.appengine.eclipse.core.gaeNature";

  private static final String GPE_WTP_GAE_FACET_ID = "com.google.appengine.facet";
  private static final String GPE_WTP_GAE_FACET_VERSION = "1";
  private static final String GPE_WTP_GAE_EAR_FACET_ID = "com.google.appengine.facet.ear";
  private static final String GPE_WTP_GAE_EAR_FACET_VERSION = "1";

  private static final String GPE_WTP_SERVER_RUNTIME = "com.google.appengine.runtime.id1";
  private static final String GPE_WTP_SERVER_RUNTIME_COMPONENT = "com.google.appengine.runtime.id";

  public static void removeObsoleteGpeFixtures(
      IFacetedProject facetedProject, IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 40);

    NatureUtils.removeNature(facetedProject.getProject(), GPE_GAE_NATURE_ID);
    subMonitor.worked(10);

    Set<IRuntime> runtimes = facetedProject.getTargetedRuntimes();
    for (IRuntime runtime : runtimes) {
      if (GPE_WTP_SERVER_RUNTIME.equals(runtime.getProperty("id"))
          || GPE_WTP_SERVER_RUNTIME_COMPONENT.equals(runtime.getProperty("id"))) {
        facetedProject.removeTargetedRuntime(runtime, null /* monitor */);
      }
    }
    subMonitor.worked(10);

    IProjectFacet gpeGaeFacet = ProjectFacetsManager.getProjectFacet(GPE_WTP_GAE_FACET_ID);
    IProjectFacetVersion gpeGaeFacetVersion = gpeGaeFacet.getVersion(GPE_WTP_GAE_FACET_VERSION);
    facetedProject.uninstallProjectFacet(gpeGaeFacetVersion, null /* config */, null /* monitor */);
    subMonitor.worked(10);

    IProjectFacet gpeGaeEarFacet = ProjectFacetsManager.getProjectFacet(GPE_WTP_GAE_EAR_FACET_ID);
    IProjectFacetVersion gpeGaeEarFacetVersion =
        gpeGaeEarFacet.getVersion(GPE_WTP_GAE_EAR_FACET_VERSION);
    facetedProject.uninstallProjectFacet(
        gpeGaeEarFacetVersion, null /* config */, null /* monitor */);
    subMonitor.worked(10);
  }
}
