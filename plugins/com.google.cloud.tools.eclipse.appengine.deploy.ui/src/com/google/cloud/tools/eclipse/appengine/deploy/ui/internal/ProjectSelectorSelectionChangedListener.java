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
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.AppEngine;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.net.UrlEscapers;
import java.text.MessageFormat;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class ProjectSelectorSelectionChangedListener implements ISelectionChangedListener {

  private static String CREATE_APP_LINK =
      "https://console.cloud.google.com/appengine/create?lang=java&project={0}&authuser={1}";

  private final AccountSelector accountSelector;
  private final ProjectRepository projectRepository;
  private final ProjectSelector projectSelector;

  @VisibleForTesting
  Job latestQueryJob;
  private Predicate<Job> isLatestQueryJob = new Predicate<Job>() {
    @Override
    public boolean apply(Job job) {
      return job == latestQueryJob;
    }
  };

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
    latestQueryJob = null;

    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
    if (selection.isEmpty()) {
      return;
    }

    GcpProject project = (GcpProject) selection.getFirstElement();
    String email = accountSelector.getSelectedEmail();
    String createAppLink = MessageFormat.format(CREATE_APP_LINK,
        project.getId(), UrlEscapers.urlFormParameterEscaper().escape(email));

    boolean queryCached = project.hasAppEngineInfo();
    if (queryCached) {
      if (project.getAppEngine() == AppEngine.NO_APPENGINE_APPLICATION) {
        projectSelector.setStatusLink(
            Messages.getString("projectselector.missing.appengine.application.link", createAppLink),
            createAppLink /* tooltip */);
      }
    } else {  // The project has never been queried.
      Credential credential = accountSelector.getSelectedCredential();
      latestQueryJob = new AppEngineApplicationQueryJob(project, credential, projectRepository,
          projectSelector, createAppLink, isLatestQueryJob);
      latestQueryJob.schedule();
    }
  }
}
