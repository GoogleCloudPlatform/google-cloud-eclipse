package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

public class JavaPackageValidator {

  /**
   * Check if a string is a legal Java package name.
   */
  public static IStatus validate(String packageName) {
    
    if (packageName == null) {
      return new Status(1, "pluginId", 45, "null package name", null);
    } else if (packageName.isEmpty()) { // default package is allowed
      return Status.OK_STATUS;
    } else if (packageName.endsWith(".")) {
      // todo or allow this and strip the period
      return new Status(1, "pluginId", 46, packageName + " ends with a period.", null);
    } else if (containsWhitespace(packageName)) {
      // very weird condition because validatePackageName allows internal white space
      return new Status(1, "pluginId", 46, packageName + " contains whitespace.", null);
    } else {
      return JavaConventions.validatePackageName(
          packageName, JavaCore.VERSION_1_4, JavaCore.VERSION_1_4);
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
