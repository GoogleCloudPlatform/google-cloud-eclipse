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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import com.google.cloud.tools.eclipse.appengine.deploy.DeployJob;
import com.google.cloud.tools.eclipse.ui.util.MessageConsoleUtilities.ConsoleFactory;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;

public class DeployConsole extends MessageConsole {

  static final String PROPERTY_JOB = DeployConsole.class.getName() + ".job";

  private static final String TYPE = "com.google.cloud.tools.eclipse.appengine.deploy.consoleType";

  private DeployJob job;

  private DeployConsole(String name) {
    super(name, null);
    setType(TYPE);
  }

  DeployJob getJob() {
    return job;
  }

  public void setJob(DeployJob newJob) {
    DeployJob oldJob = job;
    job = newJob;
    firePropertyChange(this, PROPERTY_JOB, oldJob, newJob);
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
          @Override
          public void run() {
            setName(Messages.getString("job.terminated.template", getName()));
          }
        });
      }
    });
  }

  public static class Factory implements ConsoleFactory<DeployConsole> {
    @Override
    public DeployConsole createConsole(String name) {
      return new DeployConsole(name);
    }
  }

}
