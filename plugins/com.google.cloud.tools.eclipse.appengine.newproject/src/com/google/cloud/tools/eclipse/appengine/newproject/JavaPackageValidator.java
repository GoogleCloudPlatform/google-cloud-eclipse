package com.google.cloud.tools.eclipse.appengine.newproject;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

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
        if (isJavaKeyword(parts[i])) {
          return false;
        }
      }
    }
    return true;
  }
  
  private static final Set<String> KEYWORDS = ImmutableSet.of(
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
      "class", "const", "continue", "default", "do", "double", "else", "extends",
      "false", "final", "finally", "float", "for", "goto", "if", "implements", "import",
      "instanceof", "int", "interface", "long", "native", "new", "null", "package",
      "private", "protected", "public", "return", "short", "static", "strictfp",  
      "super", "switch", "synchronized", "this", "throw", "throws", "transient",
      "true", "try", "void", "volatile", "while");

  private static boolean isJavaKeyword(String name) {
    return KEYWORDS.contains(name);
  }

  private static boolean isValidJavaName(String name) {
    if (name == null || name.isEmpty()) {
      return false;
    }
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
