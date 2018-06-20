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

import com.google.common.base.Strings;
import java.lang.reflect.InvocationTargetException;
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

  /** Create a new {@link IStatus} of the given severity. */
  public static IStatus create(int severity, Object origin, String message) {
    return new Status(severity, getBundleId(origin), message);
  }

  /** Create a new {@link IStatus} of the given severity. */
  public static IStatus create(int severity, Object origin, String message, Throwable error) {
    return new Status(severity, getBundleId(origin), message, error);
  }

  public static IStatus error(Object origin, String message) {
    return new Status(IStatus.ERROR, getBundleId(origin), message);
  }

  public static IStatus error(Object origin, String message, int code) {
    return new Status(IStatus.ERROR, getBundleId(origin), code, message, null);
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

  public static MultiStatus multi(Object origin, String message) {
    return new MultiStatus(getBundleId(origin), 0, message, null);
  }

  public static MultiStatus multi(Object origin, String message, Throwable error) {
    return new MultiStatus(getBundleId(origin), 0, message, error);
  }

  public static MultiStatus multi(Object origin, String message, IStatus[] statuses) {
    MultiStatus multiStatus = multi(origin, message);
    for (IStatus status : statuses) {
      multiStatus.add(status);
    }
    return multiStatus;
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
    return setErrorStatus(origin, message +  ": " + status.getMessage(), status.getException());
  }

  public static IStatus setErrorStatus(Object origin, String message, Throwable exception) {
    Throwable targetException = exception;
    if (exception instanceof InvocationTargetException && exception.getCause() != null) {
      targetException = targetException.getCause();
    }

    if (targetException != null && !Strings.isNullOrEmpty(targetException.getMessage())) {
      message += ": " + targetException.getMessage();
    }
    IStatus status = error(origin, message, targetException);
    StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
    return status;
  }

  /**
   * Return a simplified status by discarding all OK child statuses.
   */
  public static IStatus filter(IStatus status) {
    if (!status.isMultiStatus()) {
      return status;
    } else if (status.isOK()) {
      // return OK_STATUS to avoids oddities like Progress View showing the MultiStatus's
      // error message
      return Status.OK_STATUS;
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
