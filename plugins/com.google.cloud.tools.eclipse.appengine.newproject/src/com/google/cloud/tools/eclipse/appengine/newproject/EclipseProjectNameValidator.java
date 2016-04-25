package com.google.cloud.tools.eclipse.appengine.newproject;

// There appear to be very few limits on legal project names. Looks like
// any legal directory name will work.
public class EclipseProjectNameValidator {

  /**
   * Check if a string is a legal Eclipse project name.
   */
  public static boolean validate(String name) {
    if (name == null || name.trim().isEmpty()) {
      return false;
    } else if (name.contains("/")) {
      return false;
    }
    return true;
  }

}
