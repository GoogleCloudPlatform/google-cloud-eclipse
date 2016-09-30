package com.google.cloud.tools.eclipse.appengine.libraries;

/**
 * Represents a pattern that is used to hide classes and packages from a jar file this filter is associated with.
 */
public class ExclusionFilter extends Filter {

  public ExclusionFilter(String pattern) {
    super(pattern);
  }

}
