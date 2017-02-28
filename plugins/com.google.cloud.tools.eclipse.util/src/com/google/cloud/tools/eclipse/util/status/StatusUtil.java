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
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Utility functions to simplify creating {@link Status} objects. If
 * {@link FrameworkUtil#getBundle(Class)} does not return a bundle, then {@link Class#getName()} of
 * the origin object will be used.
 */
public class StatusUtil {

  private StatusUtil() {}

  public static IStatus error(Object origin, String message) {
    return new Status(IStatus.ERROR, getBundleId(origin), message);
  }

  public static IStatus error(Object origin, String message, Throwable error) {
    return new Status(IStatus.ERROR, getBundleId(origin), message, error);
  }

  public static IStatus warn(Object origin, String message) {
    return new Status(IStatus.WARNING, getBundleId(origin), message);
  }

  public static IStatus warn(Object origin, String message, Throwable error) {
    return new Status(IStatus.WARNING, getBundleId(origin), message, error);
  }

  public static IStatus info(Object origin, String message) {
    return new Status(IStatus.INFO, getBundleId(origin), message);
  }

  public static IStatus info(Object origin, String message, Throwable error) {
    return new Status(IStatus.INFO, getBundleId(origin), message, error);
  }

  public static MultiStatus multi(Object origin, String message) {
    return new MultiStatus(getBundleId(origin), 0, message, null);
  }

  public static MultiStatus multi(Object origin, String message, Throwable error) {
    return new MultiStatus(getBundleId(origin), 0, message, error);
  }

  private static String getBundleId(Object origin) {
    Class<?> clazz = null;
    if (origin == null) {
      clazz = StatusUtil.class;
    } else if (origin instanceof Class<?>) {
      clazz = (Class<?>) origin;
    } else {
      clazz = origin.getClass();
    }

    Bundle bundle = FrameworkUtil.getBundle(clazz);
    if (bundle == null) {
      return clazz.getName(); // what else can we do?
    }
    return bundle.getSymbolicName();
  }

  public static IStatus merge(IStatus status, IStatus newStatus) {
    if (status == null) {
      return newStatus;
    } else {
      if (status instanceof MultiStatus) {
        ((MultiStatus) status).merge(newStatus);
        return status;
      } else {
        MultiStatus merged = new MultiStatus(status.getPlugin(), status.getCode(),
            status.getMessage(), status.getException());
        merged.merge(newStatus);
        return merged;
      }
    }
    
  }

  public static IStatus setErrorStatus(Object origin, String message, IStatus status) {
    return setErrorStatus(origin, message +  " - " + status.getMessage(), status.getException());
  }

  public static IStatus setErrorStatus(Object origin, String message, Throwable ex) {
    if (ex != null && ex.getMessage() != null && !ex.getMessage().isEmpty()) {
      message += ": " + ex.getMessage();
    }
    IStatus status = error(origin, message, ex);
    StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
    return status;
  }

  /**
   * Return a simplified status by discarding all OK child statuses.
   */
  public static IStatus filter(IStatus status) {
    if (!status.isMultiStatus()) {
      return status;
    }
    MultiStatus newStatus = new MultiStatus(status.getPlugin(), status.getCode(),
        status.getMessage(), status.getException());
    for (IStatus child : status.getChildren()) {
      if (!child.isOK()) {
        newStatus.add(filter(child));
      }
    }
    return newStatus;
  }
}
