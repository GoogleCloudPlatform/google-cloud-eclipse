package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployJob;
import com.google.cloud.tools.eclipse.ui.util.console.TaggedMessageConsole;

public class DeployConsole extends TaggedMessageConsole<StandardDeployJob> {

  private static final String TYPE = "com.google.cloud.tools.eclipse.appengine.deploy.consoleType";

  public DeployConsole(String name, StandardDeployJob tag) {
    super(name, tag);
    setType(TYPE);
  }

}
