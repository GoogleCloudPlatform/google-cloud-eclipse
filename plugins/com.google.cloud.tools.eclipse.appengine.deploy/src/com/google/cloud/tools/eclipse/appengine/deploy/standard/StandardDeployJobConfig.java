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
