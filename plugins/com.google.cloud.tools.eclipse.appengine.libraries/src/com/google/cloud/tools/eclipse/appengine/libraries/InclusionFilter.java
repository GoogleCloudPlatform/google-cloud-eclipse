package com.google.cloud.tools.eclipse.appengine.libraries;

/**
 * Represents a pattern that is used to make visible classes and packages from a jar file this filter is associated
 * with.
 */
public class InclusionFilter extends Filter {

  public InclusionFilter(String pattern) {
    super(pattern);
  }
}
