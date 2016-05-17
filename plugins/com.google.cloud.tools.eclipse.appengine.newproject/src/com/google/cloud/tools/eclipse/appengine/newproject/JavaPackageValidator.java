package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

public class JavaPackageValidator {

  /**
   * Check if a string is a legal Java package name.
   */
  // todo return an IStatus for better error reporting
  public static boolean validate(String packageName) {
    if (packageName == null) {
      return false;
    } else if (packageName.isEmpty()) {
      return true;
    } else if (packageName.endsWith(".")) {
      // todo or allow this and strip the period
      return false;
    } else if (containsWhitespace(packageName)) {
      // note very weird condition because validatePackageName allows internal white space
      return false;
    } else {
      return JavaConventions.validatePackageName(
          packageName, JavaCore.VERSION_1_4, JavaCore.VERSION_1_4).isOK();
    }
  }
  
  /**
   * Checks whether this string contains any C0 controls (characters with code 
   * point <= 0x20). This is the definition of white space used by String.trim()
   * which is what we're prechecking here.
   */
  private static boolean containsWhitespace(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) <= 0x20) {
        return true;
      }
    }
    return false;
  }

}
