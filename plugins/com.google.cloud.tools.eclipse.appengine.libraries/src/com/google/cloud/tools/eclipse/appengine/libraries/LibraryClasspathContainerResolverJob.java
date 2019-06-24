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

package com.google.cloud.tools.eclipse.appengine.libraries;

import com.google.common.base.Preconditions;
import java.util.logging.Logger;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;

public class LibraryClasspathContainerResolverJob extends Job {
  private static final Logger logger =
      Logger.getLogger(LibraryClasspathContainerResolverJob.class.getName());

  private ILibraryClasspathContainerResolverService resolverService;
  private IJavaProject javaProject;

  public LibraryClasspathContainerResolverJob(
      ISchedulingRule rule,
      ILibraryClasspathContainerResolverService service,
      IJavaProject javaProject) {
    super(Messages.getString("AppEngineLibraryContainerResolverJobName"));
    // This job must be protected; our lower-level Maven classes actions do more verification
    Preconditions.checkNotNull(rule, "rule must be prvided");
    Preconditions.checkNotNull(javaProject, "javaProject is null");
    this.resolverService = service;
    this.javaProject = javaProject;
    setRule(rule);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    // may have been deleted before this job runs
    if (!javaProject.exists()) {
      logger.warning("Project no longer exists: " + javaProject.getElementName());
      return Status.OK_STATUS;
    }
    return resolverService.resolveAll(javaProject, monitor);
  }

  @Override
  public boolean belongsTo(Object family) {
    // Consider container resolution as part of builds.  Simplifies
    // our tests using {@code ProjectUtils.waitForProjects()}.
    return family == ResourcesPlugin.FAMILY_MANUAL_BUILD;
  }
  
  
}
