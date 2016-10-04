package com.google.cloud.tools.eclipse.appengine.libraries;

import com.google.common.base.Preconditions;

public class Filter {

  private String pattern;
  private boolean exclude;

  /**
   * @param pattern expected format is the same as ANT path pattern expressions:
   * http://ant.apache.org/manual/dirtasks.html#patterns
   */
  public static Filter exclusionFilter(String pattern) {
    return new Filter(pattern, true /* exclude */);
  }

  /**
   * @param pattern expected format is the same as ANT path pattern expressions:
   * http://ant.apache.org/manual/dirtasks.html#patterns
   */
  public static Filter inclusionFilter(String pattern) {
    return new Filter(pattern, false /* exclude */);
  }

  private Filter(String pattern, boolean exclude) {
    Preconditions.checkNotNull(pattern, "pattern is null");
    Preconditions.checkArgument(!pattern.isEmpty(), "pattern is empty");
    this.pattern = pattern;
    this.exclude = exclude;
  }

  public String getPattern() {
    return pattern;
  }

  public boolean isExclude() {
    return exclude;
  }

}
