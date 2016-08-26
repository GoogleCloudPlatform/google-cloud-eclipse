package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;

public class StandardDeployJobConfig {

  private IPath workDirectory;
  private Credential credential;
  private IProject project;
  private ProcessOutputLineListener stdoutLineListener;
  private ProcessOutputLineListener stderrLineListener;

  public IPath getWorkDirectory() {
    return workDirectory;
  }

  public void setWorkDirectory(IPath workDirectory) {
    this.workDirectory = workDirectory;
  }

  public Credential getCredential() {
    return credential;
  }

  public void setCredential(Credential credential) {
    this.credential = credential;
  }

  public IProject getProject() {
    return project;
  }

  public void setProject(IProject project) {
    this.project = project;
  }

  public ProcessOutputLineListener getStdoutLineListener() {
    return stdoutLineListener;
  }

  public void setStdoutLineListener(ProcessOutputLineListener stdoutLineListener) {
    this.stdoutLineListener = stdoutLineListener;
  }

  public ProcessOutputLineListener getStderrLineListener() {
    return stderrLineListener;
  }

  public void setStderrLineListener(ProcessOutputLineListener stderrLineListener) {
    this.stderrLineListener = stderrLineListener;
  }
}
