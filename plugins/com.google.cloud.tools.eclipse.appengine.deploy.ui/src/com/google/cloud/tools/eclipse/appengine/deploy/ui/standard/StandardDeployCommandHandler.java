/*******************************************************************************
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
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.deploy.ui.standard;

import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.appengine.api.deploy.DefaultDeployConfiguration;
import com.google.cloud.tools.eclipse.appengine.deploy.CleanupOldDeploysJob;
import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployJob;
import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployJobConfig;
import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployPreferencesConverter;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployConsole;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployPreferencesDialog;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import com.google.cloud.tools.eclipse.appengine.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.sdk.ui.MessageConsoleWriterOutputLineListener;
import com.google.cloud.tools.eclipse.ui.util.MessageConsoleUtilities;
import com.google.cloud.tools.eclipse.ui.util.ProjectFromSelectionHelper;
import com.google.cloud.tools.eclipse.ui.util.ServiceUtils;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;
import com.google.common.annotations.VisibleForTesting;

/**
 * Command handler to deploy an App Engine web application project to App Engine Standard.
 * <p>
 * It copies the project's exploded WAR to a staging directory and then executes 
 * the staging and deploy operations provided by the App Engine Plugins Core Library.
 */
public class StandardDeployCommandHandler extends AbstractHandler {

  private static final String CONSOLE_NAME = "App Engine Deploy";

  private ProjectFromSelectionHelper helper;

  public StandardDeployCommandHandler() {
    this(new FacetedProjectHelper());
  }

  @VisibleForTesting
  StandardDeployCommandHandler(FacetedProjectHelper facetedProjectHelper) {
    this.helper = new ProjectFromSelectionHelper(facetedProjectHelper);
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    try {
      IProject project = helper.getProject(event);
      if (project != null) {
        Credential credential = loginIfNeeded(event);
        if (credential != null) {
          if (new DeployPreferencesDialog(HandlerUtil.getActiveShell(event), project).open() == Window.OK) {
            launchDeployJob(project, credential, event);
          }
        }
      }
      // return value must be null, reserved for future use
      return null;
    } catch (CoreException | IOException exception) {
      throw new ExecutionException(Messages.getString("deploy.failed.error.message"), exception); //$NON-NLS-1$
    }
  }

  private void launchDeployJob(IProject project, Credential credential, ExecutionEvent event) 
      throws IOException, ExecutionException {
    
    AnalyticsPingManager.getInstance().sendPing(
        AnalyticsEvents.APP_ENGINE_DEPLOY, AnalyticsEvents.APP_ENGINE_DEPLOY_STANDARD, null);
    
    IPath workDirectory = createWorkDirectory();

    DefaultDeployConfiguration deployConfiguration = getDeployConfiguration(project, event);
    DeployConsole messageConsole =
        MessageConsoleUtilities.createConsole(getConsoleName(deployConfiguration.getProject()),
                                              new DeployConsole.Factory());

    MessageConsoleStream outputStream = messageConsole.newMessageStream();
    StandardDeployJobConfig config = getDeployJobConfig(project, credential, event,
        workDirectory, outputStream, deployConfiguration);

    StandardDeployJob deploy = new StandardDeployJob.Builder().config(config).build();
    messageConsole.setJob(deploy);
    deploy.addJobChangeListener(new JobChangeAdapter() {

      @Override
      public void done(IJobChangeEvent event) {
        super.done(event);
        AnalyticsPingManager.getInstance().sendPing(AnalyticsEvents.APP_ENGINE_DEPLOY_SUCCESS,
            AnalyticsEvents.APP_ENGINE_DEPLOY_STANDARD, null);
        launchCleanupJob();
      }
    });
    deploy.schedule();
  }

  private String getConsoleName(String project) {
    Date now = new Date();
    String nowString = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault()).format(now);
    return MessageFormat.format("{0} - {1} ({2})", CONSOLE_NAME, project, nowString);
  }

  private StandardDeployJobConfig getDeployJobConfig(IProject project, Credential credential,
      ExecutionEvent event, IPath workDirectory, MessageConsoleStream outputStream,
      DefaultDeployConfiguration deployConfiguration) {
    StandardDeployJobConfig config = new StandardDeployJobConfig();
    config.setProject(project)
        .setCredential(credential)
        .setWorkDirectory(workDirectory)
        .setStdoutLineListener(new MessageConsoleWriterOutputLineListener(outputStream))
        .setStderrLineListener(new MessageConsoleWriterOutputLineListener(outputStream))
        .setDeployConfiguration(deployConfiguration);
    return config;
  }

  private DefaultDeployConfiguration getDeployConfiguration(IProject project,
                                                            ExecutionEvent event) throws ExecutionException {
    StandardDeployPreferences deployPreferences = new StandardDeployPreferences(project);
    if (deployPreferences.getProjectId() == null || deployPreferences.getProjectId().isEmpty()) {
      throw new ExecutionException(Messages.getString("error.projectId.missing"));
    }
    return new StandardDeployPreferencesConverter(deployPreferences).toDeployConfiguration();
  }

  private IPath createWorkDirectory() throws IOException {
    String now = Long.toString(System.currentTimeMillis());
    IPath workDirectory = getTempDir().append(now);
    Files.createDirectories(workDirectory.toFile().toPath());
    return workDirectory;
  }

  private Credential loginIfNeeded(ExecutionEvent event) {
    IGoogleLoginService loginService = ServiceUtils.getService(event, IGoogleLoginService.class);
    Credential credential = loginService.getCachedActiveCredential();
    if (credential != null) {
      return credential;
    }

    // GoogleLoginService takes care of displaying error messages; no need to check errors.
    return loginService.getActiveCredential(Messages.getString("deploy.login.dialog.message"));
  }

  private void launchCleanupJob() {
    new CleanupOldDeploysJob(getTempDir()).schedule();
  }

  private IPath getTempDir() {
    return Platform.getStateLocation(Platform.getBundle("com.google.cloud.tools.eclipse.appengine.deploy"))
        .append("tmp");
  }
}