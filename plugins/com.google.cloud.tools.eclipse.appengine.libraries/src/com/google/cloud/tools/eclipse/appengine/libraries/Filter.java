package com.google.cloud.tools.eclipse.appengine.libraries;

public abstract class Filter {

  private String pattern;

  public Filter(String pattern) {
    this.pattern = pattern;
  }

  public String getPattern() {
    return pattern;
  }
}
