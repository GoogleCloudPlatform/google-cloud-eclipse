package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;

import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployJob;
import com.google.cloud.tools.eclipse.ui.util.console.CustomConsolePageParticipant;

public class DeployConsolePageParticipant extends CustomConsolePageParticipant<StandardDeployJob> {

  
  public DeployConsolePageParticipant() {
    closeVisible = true;
  }

  @Override
  protected void runTerminateAction() {
    StandardDeployJob deployJob = findJob();
    if (deployJob != null) {
      deployJob.cancel();
    }
  }

  private StandardDeployJob findJob() {
    Job[] jobs = Job.getJobManager().find(StandardDeployJob.FAMILY);
    for (Job job : jobs) {
      StandardDeployJob deployJob = (StandardDeployJob) job;
      if (getConsole().getTag().equals(deployJob)) {
        return deployJob;
      }
    }
    return null;
  }

  @Override
  protected boolean terminateEnabled(StandardDeployJob job) {
    return job.getState() != Job.NONE;
  }

  @Override
  protected boolean closeEnabled(StandardDeployJob job) {
    return job.getState() == Job.NONE;
  }

  @Override
  protected void initInternal() {
    StandardDeployJob deployJob = findJob();
    if (deployJob != null) {
      deployJob.addJobChangeListener(new IJobChangeListener() {
        
        @Override
        public void sleeping(IJobChangeEvent event) {
          update();
        }
        
        @Override
        public void scheduled(IJobChangeEvent event) {
          update();
        }
        
        @Override
        public void running(IJobChangeEvent event) {
          update();
        }
        
        @Override
        public void done(IJobChangeEvent event) {
          update();
        }
        
        @Override
        public void awake(IJobChangeEvent event) {
          update();
        }
        
        @Override
        public void aboutToRun(IJobChangeEvent event) {
          update();
        }
      });
    }
  }
}
