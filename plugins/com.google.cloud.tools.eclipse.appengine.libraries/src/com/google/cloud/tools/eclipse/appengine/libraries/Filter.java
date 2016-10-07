package com.google.cloud.tools.eclipse.appengine.libraries;

import org.eclipse.jdt.core.IClasspathEntry;

import com.google.common.base.Preconditions;

public class Filter {

  private String pattern;
  private boolean exclude;

  /**
   * @param pattern expected format is the same as JDT's build path inclusion/exclusion filters.
   *
   * @see IClasspathEntry#getExclusionPatterns()
   */
  public static Filter exclusionFilter(String pattern) {
    return new Filter(pattern, true /* exclude */);
  }

  /**
   * @param pattern expected format is the same as JDT's build path inclusion/exclusion filters.
   *
   * @see IClasspathEntry#getInclusionPatterns()
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

  /**
   * @return a pattern in the format that is the same as JDT's build path inclusion/exclusion filters.
   *
   * @see IClasspathEntry#getInclusionPatterns()
   */
  public String getPattern() {
    return pattern;
  }

  public boolean isExclude() {
    return exclude;
  }

}
