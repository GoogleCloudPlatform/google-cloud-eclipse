package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.appengine.deploy.AppEngineProjectDeployer;
import com.google.cloud.tools.eclipse.appengine.deploy.Messages;
import com.google.cloud.tools.eclipse.sdk.ui.MessageConsoleWriterOutputLineListener;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.cloud.tools.eclipse.util.MessageConsoleUtilities;
import com.google.common.base.Preconditions;

/**
 * Executes a job that deploys a project to App Engine Standard.
 * <p>
 * Deploy steps:
 * <ol>
 *  <li>export exploded WAR</li>
 *  <li>stage project for deploy</li>
 *  <li>deploy staged project</li>
 * </ol>
 * It uses a work directory where it will create separate directories for the exploded WAR and the
 * staging results.
 */
public class StandardDeployJob extends WorkspaceJob {

  private static final String STAGING_DIRECTORY_NAME = "staging";
  private static final String EXPLODED_WAR_DIRECTORY_NAME = "exploded-war";
  private static final String CONSOLE_NAME = "App Engine Deploy";


  private final ExplodedWarPublisher exporter;
  private final StandardProjectStaging staging;
  private AppEngineProjectDeployer deployer;
  private final IProject project;
  private final IPath workDirectory;

  public StandardDeployJob(ExplodedWarPublisher exporter,
                           StandardProjectStaging staging,
                           AppEngineProjectDeployer deployer,
                           IPath workDirectory,
                           IProject project) {
    super(Messages.getString("deploy.standard.runnable.name")); //$NON-NLS-1$

    Preconditions.checkNotNull(exporter, "exporter is null");
    Preconditions.checkNotNull(staging, "staging is null");
    Preconditions.checkNotNull(workDirectory, "workDirectory is null");
    Preconditions.checkNotNull(project, "project is null");

    setRule(project);
    this.exporter = exporter;
    this.staging = staging;
    this.deployer = deployer;
    this.project = project;
    this.workDirectory = workDirectory;
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 100);
    try {
      IPath explodedWarDirectory = workDirectory.append(EXPLODED_WAR_DIRECTORY_NAME);
      IPath stagingDirectory = workDirectory.append(STAGING_DIRECTORY_NAME);
      CloudSdk cloudSdk = getCloudSdk();

      exporter.publish(project, explodedWarDirectory, progress.newChild(10));
      staging.stage(explodedWarDirectory, stagingDirectory, cloudSdk, progress.newChild(20));
      deployer.deploy(stagingDirectory, cloudSdk, progress.newChild(70));

      return Status.OK_STATUS;
    } finally {
      monitor.done();
    }
  }

  private CloudSdk getCloudSdk() {
    MessageConsole messageConsole =
        MessageConsoleUtilities.getMessageConsole(CONSOLE_NAME, null, true /* show */);
    final MessageConsoleStream outputStream = messageConsole.newMessageStream();
    CloudSdk cloudSdk = new CloudSdk.Builder()
                          .addStdOutLineListener(new MessageConsoleWriterOutputLineListener(outputStream))
                          .addStdErrLineListener(new MessageConsoleWriterOutputLineListener(outputStream))
                          .appCommandMetricsEnvironment(CloudToolsInfo.METRICS_NAME)
                          .appCommandMetricsEnvironmentVersion(CloudToolsInfo.getToolsVersion())
                          .build();
    return cloudSdk;
  }
}
