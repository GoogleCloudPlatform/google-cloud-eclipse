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
import com.google.common.annotations.VisibleForTesting;
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
import org.eclipse.jst.j2ee.refactor.listeners.J2EEElementChangedListener;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class StandardFacetInstallDelegate extends AppEngineFacetInstallDelegate {
  private static final String APPENGINE_WEB_XML = "appengine-web.xml";

  private static final String JSDT_FACET_ID = "wst.jsdt.web";
  private static final int MAX_JSDT_CHECK_RETRIES = 100;

  @Override
  public void execute(IProject project,
                      IProjectFacetVersion version,
                      Object config,
                      IProgressMonitor monitor) throws CoreException {
    super.execute(project, version, config, monitor);
    createConfigFiles(project, monitor);
    installAppEngineRuntimes(project);
  }

  private void installAppEngineRuntimes(IProject project) throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);

    // Modifying targeted runtimes while installing/uninstalling facets is not allowed,
    // so schedule a job as a workaround.
    Job installJob = new AppEngineRuntimeInstallJob(facetedProject);
    // Schedule immediately so that it doesn't go into the SLEEPING state. Ensuring the job is
    // active is necessary for unit testing.
    installJob.schedule();
    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1155
    // The first ConvertJob has already been scheduled (which installs JSDT facet), and
    // this is to suspend the second ConvertJob temporarily.
    NonSystemJobSuspender.suspendFutureJobs();
  }

  private static class AppEngineRuntimeInstallJob extends Job {

    private IFacetedProject facetedProject;

    private AppEngineRuntimeInstallJob(IFacetedProject facetedProject) {
      super(Messages.getString("appengine.install.runtime.to.project", // $NON-NLS$
          facetedProject.getProject().getName()));
      this.facetedProject = facetedProject;
    }

    /**
     * Mark this job as a component update job. Useful for our tests to ensure project configuration
     * is complete.
     */
    @Override
    public boolean belongsTo(Object family) {
      return J2EEElementChangedListener.PROJECT_COMPONENT_UPDATE_JOB_FAMILY.equals(family);
    }

    private void waitUntilJsdtIsFixedFacet() {
      try {
        IProjectFacet jsdtFacet = ProjectFacetsManager.getProjectFacet(JSDT_FACET_ID);
        for (int times = 0;
            times < MAX_JSDT_CHECK_RETRIES && !facetedProject.isFixedProjectFacet(jsdtFacet);
            times++) {
          try {
            // To prevent going into the SLEEPING state, don't use "Job.schedule(100)".
            Thread.sleep(100 /* ms */);
          } catch (InterruptedException ex) {
            // Keep waiting.
          }
        }
      } catch (IllegalArgumentException ex) {
        // JSDT facet itself doesn't exist. (Should not really happen.) Ignore and fall through.
      }
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      try {
        // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1155
        // Wait until the first ConvertJob installs the JSDT facet.
        waitUntilJsdtIsFixedFacet();
        AppEngineStandardFacet.installAllAppEngineRuntimes(facetedProject, monitor);
        return Status.OK_STATUS;
      } catch (CoreException ex) {
        return ex.getStatus();
      } finally {
        // Now resume all the suspended jobs (including the second ConvertJob).
        NonSystemJobSuspender.resume();
      }
    }
  }

  /**
   * Creates an appengine-web.xml file in the WEB-INF folder if it doesn't exist.
   */
  @VisibleForTesting
  void createConfigFiles(IProject project, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 10);

    // The virtual component model is very flexible, but we assume that
    // the WEB-INF/appengine-web.xml isn't a virtual file remapped elsewhere
    IFolder webInfDir = WebProjectUtil.getWebInfDirectory(project);
    
    if (webInfDir == null) {
      webInfDir =
          project.getFolder(WebProjectUtil.DEFAULT_WEB_PATH).getFolder(WebProjectUtil.WEB_INF);
      ResourceUtils.createFolders(webInfDir, progress.newChild(3));
    }
    
    progress.worked(1);
    
    IFile appEngineWebXml = webInfDir.getFile(APPENGINE_WEB_XML);

    if (appEngineWebXml.exists()) {
      return;
    }

    appEngineWebXml.create(new ByteArrayInputStream(new byte[0]), true, progress.newChild(2));
    String configFileLocation = appEngineWebXml.getLocation().toString();
    AppEngineTemplateUtility.createFileContent(
        configFileLocation, AppEngineTemplateUtility.APPENGINE_WEB_XML_TEMPLATE,
        Collections.<String, String>emptyMap());
    progress.worked(4);
    appEngineWebXml.refreshLocal(IFile.DEPTH_ZERO, progress.newChild(1));
  }
}
