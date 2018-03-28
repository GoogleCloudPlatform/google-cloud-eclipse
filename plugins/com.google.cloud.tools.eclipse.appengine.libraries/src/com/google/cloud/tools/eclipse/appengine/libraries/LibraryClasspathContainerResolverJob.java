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
import javax.inject.Inject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.m2e.core.MavenPlugin;

public class LibraryClasspathContainerResolverJob extends Job {
  private static final Logger logger =
      Logger.getLogger(LibraryClasspathContainerResolverJob.class.getName());

  @Inject
  private ILibraryClasspathContainerResolverService resolverService;
  private IJavaProject javaProject;

  @Inject
  public LibraryClasspathContainerResolverJob(IJavaProject javaProject) {
    super(Messages.getString("AppEngineLibraryContainerResolverJobName"));
    Preconditions.checkNotNull(javaProject, "javaProject is null");
    this.javaProject = javaProject;
    setUser(true);
    setRule(resolvingRule());
  }

  /** Return the rule for updating the project's classpath. */
  private ISchedulingRule resolvingRule() {
    // Requires both the project rule and the Maven project configuration rule
    IWorkspace workspace = javaProject.getProject().getWorkspace();
    ISchedulingRule rule =
        MultiRule.combine(
            workspace.getRuleFactory().modifyRule(javaProject.getProject()),
            MavenPlugin.getProjectConfigurationManager().getRule());
    return rule;
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
}
