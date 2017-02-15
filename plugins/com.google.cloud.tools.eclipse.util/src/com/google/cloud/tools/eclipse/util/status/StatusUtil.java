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

package com.google.cloud.tools.eclipse.util.status;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Utility functions to simplify creating {@link Status} objects. If {@link FrameworkUtil#getBundle(Class)} does not
 * return a bundle, then {@link Class#getName()} of the origin object will be used.
 */
public class StatusUtil {

  private StatusUtil() {}

  public static IStatus error(Class<?> origin, String message) {
    return error(origin, message, null);
  }

  public static IStatus error(Class<?> origin, String message, Throwable error) {
    String bundleOrClassname = null;
    
    Bundle bundle = FrameworkUtil.getBundle(origin);
    if (bundle == null) {
      bundleOrClassname = origin.getName();
    } else {
      bundleOrClassname = bundle.getSymbolicName();
    }
    return error(bundleOrClassname, message, error);
  }

  public static IStatus error(Object origin, String message) {
    if (origin instanceof Class) {
      return error((Class<?>) origin, message);
    } else {
      return error(origin.getClass(), message);
    }
  }

  public static IStatus error(Object origin, String message, Throwable error) {
    if (origin instanceof Class) {
      return error((Class<?>) origin, message, error);
    } else {
      return error(origin.getClass(), message, error);
    }
  }

  private static IStatus error(String bundleOrClassname, String message, Throwable error) {
    if (error == null) {
      return new Status(IStatus.ERROR, bundleOrClassname, message);
    } else {
      return new Status(IStatus.ERROR, bundleOrClassname, message, error);
    }
  }

}
