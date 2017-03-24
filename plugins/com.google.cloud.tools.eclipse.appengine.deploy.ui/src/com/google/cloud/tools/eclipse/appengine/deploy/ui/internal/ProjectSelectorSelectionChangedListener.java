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

package com.google.cloud.tools.eclipse.appengine.deploy.ui.internal;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.AppEngine;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.common.base.Predicate;
import com.google.common.net.UrlEscapers;
import java.text.MessageFormat;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Display;

public class ProjectSelectorSelectionChangedListener implements ISelectionChangedListener {

  private static String CREATE_APP_LINK =
      "https://console.cloud.google.com/appengine/create?lang=java&project={0}&authuser={1}";

  private final AccountSelector accountSelector;
  private final ProjectRepository projectRepository;
  private final ProjectSelector projectSelector;

  public ProjectSelectorSelectionChangedListener(AccountSelector accountSelector,
                                                 ProjectRepository projectRepository,
                                                 ProjectSelector projectSelector) {
    this.accountSelector = accountSelector;
    this.projectRepository = projectRepository;
    this.projectSelector = projectSelector;
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    projectSelector.clearStatusLink();

    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
    if (!selection.isEmpty()) {
      GcpProject project = (GcpProject) selection.getFirstElement();

      String email = accountSelector.getSelectedEmail();
      String createAppLink = MessageFormat.format(CREATE_APP_LINK,
          project.getId(), UrlEscapers.urlFormParameterEscaper().escape(email));

      boolean alreadyQueried = project.hasAppEngineInfo();
      if (alreadyQueried) {
        if (project.getAppEngine() == AppEngine.NO_APPENGINE_APPLICATION) {
          projectSelector.setStatusLink(Messages.getString(
              "projectselector.missing.appengine.application.link", createAppLink),
              createAppLink /* tooltip */);
        }
      } else {
        Display display = projectSelector.getDisplay();
        Credential credential = accountSelector.getSelectedCredential();
        Predicate<Job> p = new Predicate<Job>(){
          @Override
          public boolean apply(Job arg0) {
            return false;
          }};
        Job job = new MyJob(project, credential, projectRepository, projectSelector,
            createAppLink, p, display);
      }
    }
    /*
    } catch (ProjectRepositoryException ex) {
      projectSelector.setStatusLink(
          Messages.getString("projectselector.retrieveapplication.error.message",
                             ex.getLocalizedMessage()),
          null // tooltip
          );
    }
    */
  }
}

class MyJob extends Job {

  private final GcpProject project;
  private final Credential credential;
  private final ProjectRepository projectRepository;
  private final ProjectSelector projectSelector;
  private final Predicate<Job> isLatestAppQueryJob;
  private final String createAppLink;
  private final Display display;

  /**
   * @param projectRepository {@link ProjectRepository#getAppEngineApplication} must be thread-safe
   * @param isLatestAppQueryJob executed in the UI context
   * @param display
   */
  public MyJob(GcpProject project, Credential credential, ProjectRepository projectRepository,
      ProjectSelector projectSelector, String createAppLink,
      Predicate<Job> isLatestAppQueryJob, Display display) {
    super("Checking GCP project has App Engine Application...");
    this.project = project;
    this.credential = credential;
    this.projectRepository = projectRepository;
    this.projectSelector = projectSelector;
    this.createAppLink = createAppLink;
    this.isLatestAppQueryJob = isLatestAppQueryJob;
    this.display = display;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    AppEngine appEngine = null;
    String statusMessage = null;
    String statusTooltip = null;

    try {
      // The original design was to cache the returned "appEngine" right after querying, but
      // because we are in a non-UI thread, defer caching until we get into the UI thread.
      appEngine = projectRepository.getAppEngineApplication(credential, project.getId());

      if (appEngine == AppEngine.NO_APPENGINE_APPLICATION) {
        statusMessage = Messages.getString(
            "projectselector.missing.appengine.application.link", createAppLink);
        statusTooltip = createAppLink;
      }
    } catch (ProjectRepositoryException ex) {
      statusMessage = Messages.getString(
          "projectselector.retrieveapplication.error.message", ex.getLocalizedMessage());
    }

    if (appEngine != null || statusMessage != null) {
      updateInUiThread(appEngine, statusMessage, statusTooltip);
    }
    return Status.OK_STATUS;
  }

  private void updateInUiThread(final AppEngine appEngine,
      final String statusMessage, final String statusTooltip) {
    final Job thisJob = this;

    // The selector may have been disposed (i.e., dialog closed), so check it in the UI thread.
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        if (!projectSelector.isDisposed()
            && isLatestAppQueryJob.apply(thisJob) /* intentionally checking in UI context */) {
          project.setAppEngine(appEngine);
          projectSelector.setStatusLink(statusMessage, statusTooltip);
        }
      }
    });
  }
}
