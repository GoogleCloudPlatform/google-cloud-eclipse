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

package com.google.cloud.tools.eclipse.appengine.facets;

import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import com.google.cloud.tools.eclipse.util.templates.appengine.AppEngineTemplateUtility;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class StandardFacetInstallDelegate extends AppEngineFacetInstallDelegate {
  private final static String APPENGINE_WEB_XML = "appengine-web.xml";

  @Override
  public void execute(IProject project,
                      IProjectFacetVersion version,
                      Object config,
                      IProgressMonitor monitor) throws CoreException {
    super.execute(project, version, config, monitor);
    createConfigFiles(project, monitor);
    installAppEngineRuntimes(project);
  }

  private void installAppEngineRuntimes(final IProject project) {
    // Modifying targeted runtimes while installing/uninstalling facets is not allowed,
    // so schedule a job as a workaround.
    Job installJob = new Job("Install App Engine runtimes in " + project.getName()) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          IFacetedProject facetedProject = ProjectFacetsManager.create(project);
          AppEngineStandardFacet.installAllAppEngineRuntimes(facetedProject, monitor);
          return Status.OK_STATUS;
        } catch (CoreException ex) {
          return ex.getStatus();
        }
      }
    };
    installJob.schedule();
  }

  /**
   * Creates an appengine-web.xml file in the WEB-INF folder if it doesn't exist
   */
  private void createConfigFiles(IProject project, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 10);

    // The virtual component model is very flexible, but we assume that
    // the WEB-INF/appengine-web.xml isn't a virtual file remapped elsewhere
    IFolder webInfDir = WebProjectUtil.getWebInfDirectory(project);
    IFile appEngineWebXml = webInfDir.getFile(APPENGINE_WEB_XML);

    if (appEngineWebXml.exists()) {
      return;
    }

    ResourceUtils.createFolders(webInfDir, progress.newChild(2));

    appEngineWebXml.create(new ByteArrayInputStream(new byte[0]), true, progress.newChild(2));
    String configFileLocation = appEngineWebXml.getLocation().toString();
    AppEngineTemplateUtility.createFileContent(
        configFileLocation, AppEngineTemplateUtility.APPENGINE_WEB_XML_TEMPLATE,
        Collections.<String, String>emptyMap());
    progress.worked(6);
  }
}
