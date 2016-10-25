/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.newproject;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

public class JavaPackageValidator {

  private static final String PLUGIN_ID = 
      "com.google.cloud.tools.eclipse.appengine.newproject.AppEngineStandard";
  
  /**
   * Check if a string is a legal Java package name.
   */
  public static IStatus validate(String packageName) {
    if (packageName == null) {
      return new Status(IStatus.ERROR, PLUGIN_ID, 45, "null package name", null);
    } else if (packageName.isEmpty()) { // default package is allowed
      return Status.OK_STATUS;
    } else if (packageName.endsWith(".")) {
      // todo or allow this and strip the period
      return new Status(IStatus.ERROR, PLUGIN_ID, 46, 
          MessageFormat.format("{0} ends with a period.", packageName), null);
    } else if (containsWhitespace(packageName)) {
      // very weird condition because validatePackageName allows internal white space
      return new Status(IStatus.ERROR, PLUGIN_ID, 46, 
          MessageFormat.format("{0} contains whitespace.", packageName), null);
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
