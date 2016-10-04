package com.google.cloud.tools.eclipse.appengine.libraries;

import com.google.common.base.Preconditions;

public class Filter {

  private String pattern;

  /**
   * @param pattern expected format is the same as ANT path pattern expressions: 
   * http://ant.apache.org/manual/dirtasks.html#patterns
   */
  public Filter(String pattern) {
    Preconditions.checkNotNull(pattern, "pattern is null");
    Preconditions.checkArgument(!pattern.isEmpty(), "pattern is empty");
    this.pattern = pattern;
  }

  public String getPattern() {
    return pattern;
  }
}
