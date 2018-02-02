/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.sdk.internal;

import com.google.cloud.tools.eclipse.sdk.Messages;
import com.google.cloud.tools.eclipse.util.jobs.MutexRule;
import com.google.common.annotations.VisibleForTesting;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsoleStream;

public class CloudSdkInstallJob extends Job {

  public static final Object CLOUD_SDK_MODIFY_JOB_FAMILY = new Object();

  /** Scheduling rule to prevent running {@code CloudSdkInstallJob} concurrently. */
  @VisibleForTesting
  static final MutexRule MUTEX_RULE = new MutexRule("for " + CloudSdkInstallJob.class);

  private final MessageConsoleStream consoleStream;

  public CloudSdkInstallJob(MessageConsoleStream consoleStream) {
    super(Messages.getString("installJobName"));
    this.consoleStream = consoleStream;
    setRule(MUTEX_RULE);
  }

  @Override
  public boolean belongsTo(Object family) {
    return super.belongsTo(family) || family == CLOUD_SDK_MODIFY_JOB_FAMILY;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    // TODO(chanseok): to be implemented: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2753
    consoleStream.println("to be implemented");
    return Status.OK_STATUS;
  }
}
