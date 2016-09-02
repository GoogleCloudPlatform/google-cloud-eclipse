package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;

import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployJob;
import com.google.cloud.tools.eclipse.ui.util.MessageConsoleUtilities.ConsoleFactory;

public class DeployConsole extends MessageConsole {

  public static final String PROPERTY_JOB = DeployConsole.class.getName() + ".job";

  private static final String TYPE = "com.google.cloud.tools.eclipse.appengine.deploy.consoleType";

  private StandardDeployJob job;

  public DeployConsole(String name) {
    super(name, null);
    setType(TYPE);
  }

  public StandardDeployJob getJob() {
    return job;
  }

  public void setJob(StandardDeployJob deployJob) {
    firePropertyChange(this, PROPERTY_JOB, job, this.job = deployJob);
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
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
