package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.appengine.api.deploy.DefaultDeployConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;

public class StandardDeployJobConfig {

  private IPath workDirectory;
  private Credential credential;
  private IProject project;
  private ProcessOutputLineListener stdoutLineListener;
  private ProcessOutputLineListener stderrLineListener;
  private DefaultDeployConfiguration deployConfiguration;

  public IPath getWorkDirectory() {
    return workDirectory;
  }

  public StandardDeployJobConfig setWorkDirectory(IPath workDirectory) {
    this.workDirectory = workDirectory;
    return this;
  }

  public Credential getCredential() {
    return credential;
  }

  public StandardDeployJobConfig setCredential(Credential credential) {
    this.credential = credential;
    return this;
  }

  public IProject getProject() {
    return project;
  }

  public StandardDeployJobConfig setProject(IProject project) {
    this.project = project;
    return this;
  }

  public ProcessOutputLineListener getStdoutLineListener() {
    return stdoutLineListener;
  }

  public StandardDeployJobConfig setStdoutLineListener(ProcessOutputLineListener stdoutLineListener) {
    this.stdoutLineListener = stdoutLineListener;
    return this;
  }

  public ProcessOutputLineListener getStderrLineListener() {
    return stderrLineListener;
  }

  public StandardDeployJobConfig setStderrLineListener(ProcessOutputLineListener stderrLineListener) {
    this.stderrLineListener = stderrLineListener;
    return this;
  }

  public DefaultDeployConfiguration getDeployConfiguration() {
    return deployConfiguration;
  }

  public StandardDeployJobConfig setDeployConfiguration(DefaultDeployConfiguration deployConfiguration) {
    this.deployConfiguration = deployConfiguration;
    return this;
  }
}
