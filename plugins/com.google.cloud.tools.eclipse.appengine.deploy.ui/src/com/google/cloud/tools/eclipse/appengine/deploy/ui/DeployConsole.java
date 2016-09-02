package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.ui.console.MessageConsole;

import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployJob;
import com.google.cloud.tools.eclipse.ui.util.MessageConsoleUtilities.ConsoleFactory;

public class DeployConsole extends MessageConsole {

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
    this.job = deployJob;
  }

  public static class Factory implements ConsoleFactory<DeployConsole> {
    @Override
    public DeployConsole createConsole(String name) {
      return new DeployConsole(name);
    }
  }

}
