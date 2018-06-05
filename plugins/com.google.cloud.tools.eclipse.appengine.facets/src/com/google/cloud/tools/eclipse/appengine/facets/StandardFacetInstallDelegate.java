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

import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.cloud.tools.eclipse.util.Templates;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.j2ee.refactor.listeners.J2EEElementChangedListener;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class StandardFacetInstallDelegate implements IDelegate {
  private static final String JSDT_FACET_ID = "wst.jsdt.web";
  private static final int MAX_JSDT_CHECK_RETRIES = 100;

  @Override
  public void execute(IProject project,
                      IProjectFacetVersion version,
                      Object config,
                      IProgressMonitor monitor) throws CoreException {
    createConfigFiles(project, version, monitor);
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
    if (isEclipseNeon()) {
      // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1155
      // The first ConvertJob has already been scheduled (which installs JSDT facet), and
      // this is to suspend the second ConvertJob temporarily.
      ConvertJobSuspender.suspendFutureConvertJobs();
    }
  }

  private static boolean isEclipseNeon() {
    return CloudToolsInfo.getEclipseVersion().startsWith("4.6");
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

    private void waitUntilJsdtIsFixedFacet(IProgressMonitor monitor) throws InterruptedException {
      try {
        IProjectFacet jsdtFacet = ProjectFacetsManager.getProjectFacet(JSDT_FACET_ID);
        for (int times = 0; !monitor.isCanceled() && times < MAX_JSDT_CHECK_RETRIES; times++) {
          if (facetedProject.isFixedProjectFacet(jsdtFacet)) {
            return;
          }
          Thread.sleep(100 /* ms */);
        }
      } catch (IllegalArgumentException ex) {
        // JSDT facet itself doesn't exist. (Should not really happen.) Ignore and fall through.
      }
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      try {
        if (isEclipseNeon()) {
          // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1155
          // Wait until the first ConvertJob installs the JSDT facet.
          waitUntilJsdtIsFixedFacet(monitor);
          if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
          }
        }
        AppEngineStandardFacet.installAllAppEngineRuntimes(facetedProject, monitor);
        return Status.OK_STATUS;

      } catch (CoreException ex) {
        return ex.getStatus();
      } catch (InterruptedException ex) {
        return Status.CANCEL_STATUS;
      } finally {
        if (isEclipseNeon()) {
          // Now resume the second ConvertJob.
          ConvertJobSuspender.resume();
        }
      }
    }
  }

  void createConfigFiles(IProject project, IProjectFacetVersion facetVersion,
      IProgressMonitor monitor) throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 10);

    createFileInWebInf(project, "logging.properties", Templates.LOGGING_PROPERTIES_TEMPLATE,
        Collections.emptyMap(), progress.split(5));

    Map<String, String> parameters = new HashMap<>();
    Object appEngineRuntime = facetVersion.getProperty("appengine.runtime");
    if (appEngineRuntime instanceof String) {
      parameters.put("runtime", (String) appEngineRuntime);
    }
    createFileInWebInf(project, "appengine-web.xml", Templates.APPENGINE_WEB_XML_TEMPLATE,
        parameters, progress.split(5));
  }

  /** Creates a file in the WEB-INF folder if it doesn't exist. */
  private void createFileInWebInf(IProject project, String filename, String templateName,
      Map<String, String> templateParameters, IProgressMonitor monitor) throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 7);

    IFile targetFile = WebProjectUtil.findInWebInf(project, new Path(filename));
    if (targetFile != null && targetFile.exists()) {
      return;
    }

    // Use the virtual component model to decide where to create the file
    targetFile =
        WebProjectUtil.createFileInWebInf(
            project,
            new Path(filename),
            new ByteArrayInputStream(new byte[0]),
            false /* overwrite */,
            progress.split(2));
    String fileLocation = targetFile.getLocation().toString();
    Templates.createFileContent(fileLocation, templateName, templateParameters);
    progress.worked(4);
    targetFile.refreshLocal(IFile.DEPTH_ZERO, progress.split(1));
  }
}
