package com.google.cloud.tools.eclipse.appengine.newproject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Check if a string is a legal App Engine Project ID.
 * 
 * <pre>
 * app-id ::= [(domain):](display-app-id)
 * domain ::= r'(?!-)[a-z\d\-\.]{1,100}'
 * display-app-id ::= r'(?!-)[a-z\d\-]{1,100}'
 * </pre>
 */
public class AppEngineProjectIdValidator {

  private static final Pattern PATTERN = Pattern.compile("([a-z\\d\\-\\.]{1,100}:)?[a-z\\d\\-\\.]{1,100}");
  
  public static boolean validate(String id) {
    if (id == null) {
      return false;
    } else if (id.isEmpty()) {
      return true;
    }
    Matcher matcher = PATTERN.matcher(id);
    return matcher.matches();
  }

}
