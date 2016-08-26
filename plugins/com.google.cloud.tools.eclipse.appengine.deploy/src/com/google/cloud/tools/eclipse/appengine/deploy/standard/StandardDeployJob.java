package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.eclipse.appengine.deploy.AppEngineProjectDeployer;
import com.google.cloud.tools.eclipse.appengine.deploy.Messages;
import com.google.cloud.tools.eclipse.appengine.login.CredentialHelper;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Charsets;
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
  private static final String CREDENTIAL_FILENAME = "gcloud-credentials.json";

  private static final Logger logger = Logger.getLogger(StandardDeployJob.class.getName());

  private final ExplodedWarPublisher exporter;
  private final StandardProjectStaging staging;
  private AppEngineProjectDeployer deployer;
  
  //temporary way of error handling, after #439 is fixed, it'll be cleaner
  protected boolean cloudSdkProcessError;
  private StandardDeployJobConfig config;

  public StandardDeployJob(ExplodedWarPublisher exporter,
                           StandardProjectStaging staging,
                           AppEngineProjectDeployer deployer,
                           StandardDeployJobConfig config) {
    super(Messages.getString("deploy.standard.runnable.name")); //$NON-NLS-1$

    Preconditions.checkNotNull(deployer, "deployer is null");
    Preconditions.checkNotNull(exporter, "exporter is null");
    Preconditions.checkNotNull(staging, "staging is null");
    Preconditions.checkNotNull(config, "config is null");

    this.exporter = exporter;
    this.staging = staging;
    this.deployer = deployer;
    this.config = config;
    setRule(config.getProject());
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 100);
    Path credentialFile = null;
    try {
      IPath workDirectory = config.getWorkDirectory();
      IPath explodedWarDirectory = workDirectory.append(EXPLODED_WAR_DIRECTORY_NAME);
      IPath stagingDirectory = workDirectory.append(STAGING_DIRECTORY_NAME);
      credentialFile = workDirectory.append(CREDENTIAL_FILENAME).toFile().toPath();
      saveCredential(credentialFile, config.getCredential());
      CloudSdk cloudSdk = getCloudSdk(credentialFile);

      exporter.publish(config.getProject(), explodedWarDirectory, progress.newChild(10));
      staging.stage(explodedWarDirectory, stagingDirectory, cloudSdk, progress.newChild(20));
      if (cloudSdkProcessError) { // temporary way of error handling, after #439 is fixed, it'll be cleaner
        return StatusUtil.error(getClass(), "Staging failed, check the error message in the Console View");
      }
      deployer.deploy(stagingDirectory, cloudSdk, progress.newChild(70));
      if (cloudSdkProcessError) { // temporary way of error handling, after #439 is fixed, it'll be cleaner
        return StatusUtil.error(getClass(), "Deploy failed, check the error message in the Console View");
      }

      return Status.OK_STATUS;
    } catch (IOException exception) {
      throw new CoreException(StatusUtil.error(getClass(),
                                               Messages.getString("save.credential.failed"),
                                               exception));
    } finally {
      if (credentialFile != null) {
        try {
          Files.delete(credentialFile);
        } catch (IOException exception) {
          logger.log(Level.WARNING, "Could not delete credential file after deploy", exception);
        }
      }
      monitor.done();
    }
  }

  private void saveCredential(Path destination, Credential credential) throws IOException {
    String jsonCredential = new CredentialHelper().toJson(credential);
    Files.write(destination, jsonCredential.getBytes(Charsets.UTF_8));
  }

  private CloudSdk getCloudSdk(Path credentialFile) throws IOException {
    CloudSdk cloudSdk = new CloudSdk.Builder()
                          .addStdOutLineListener(config.getStdoutLineListener())
                          .addStdErrLineListener(config.getStderrLineListener())
                          .appCommandCredentialFile(credentialFile.toFile())
                          .exitListener(new RecordProcessError())
                          .appCommandMetricsEnvironment(CloudToolsInfo.METRICS_NAME)
                          .appCommandMetricsEnvironmentVersion(CloudToolsInfo.getToolsVersion())
                          .build();
    return cloudSdk;
  }

  private final class RecordProcessError implements ProcessExitListener {
    // temporary way of error handling, after #439 is fixed, it'll be cleaner
    @Override
    public void onExit(int exitCode) {
      if (exitCode != 0) {
        cloudSdkProcessError = true;
      }
    }
  }

}
