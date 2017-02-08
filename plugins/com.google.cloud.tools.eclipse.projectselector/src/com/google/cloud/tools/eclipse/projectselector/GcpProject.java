package com.google.cloud.tools.eclipse.projectselector;

public class GcpProject {

  private final String name;
  private final String id;

  public GcpProject(String name, String id) {
    this.name = name;
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

}
