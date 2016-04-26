package com.google.cloud.tools.eclipse.appengine.newproject;

public class JavaPackageValidator {

  /**
   * Check if a string is a legal Java package name.
   */
  public static boolean validate(String packageName) {
    if (packageName == null) {
      return false;
    } else if (packageName.isEmpty()) {
      return true;
    } else if (packageName.endsWith(".")) {
      // todo or allow this and strip the period
      return false;
    } else {
      String[] parts = packageName.split("\\.");
      for (int i = 0; i < parts.length; i++) {
        if (!isValidJavaName(parts[i])) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean isValidJavaName(String name) {
    if (!Character.isJavaIdentifierStart(name.charAt(0))) {
      return false;
    }
    for (int i = 1; i < name.length(); i++) {
      if (!Character.isJavaIdentifierPart(name.charAt(i))) {
        return false;
      }
    }
    return true;
  }

}
