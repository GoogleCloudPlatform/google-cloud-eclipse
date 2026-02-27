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

package com.google.cloud.tools.eclipse.appengine.standard.java8.m2e;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineConfigurationUtil;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Configure facets on Maven import.
 */
public class AppEngineStandardProjectDetector extends AbstractProjectConfigurator {

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    IProject project = request.getProject();
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    if (facetedProject == null || facetedProject.hasProjectFacet(AppEngineStandardFacet.FACET)) {
      return;
    }
    IFile appEngineWebXml =
        AppEngineConfigurationUtil.findConfigurationFile(
            project, new Path("appengine-web.xml")); // $NON-NLS-1$
    if (appEngineWebXml == null || !appEngineWebXml.exists()) {
      return;
    }
    AppEngineStandardFacet.installAppEngineFacet(facetedProject, true, monitor);
  }
}
