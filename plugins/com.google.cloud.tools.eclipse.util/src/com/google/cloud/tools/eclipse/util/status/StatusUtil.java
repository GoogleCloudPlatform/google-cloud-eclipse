package com.google.cloud.tools.eclipse.util.status;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;

public class StatusUtil {

  public static IStatus error(Class<?> origin, String message) {
    return new Status(IStatus.ERROR, FrameworkUtil.getBundle(origin).getSymbolicName(), message);
  }

  public static IStatus error(Class<?> origin, String message, Throwable error) {
    return new Status(IStatus.ERROR, FrameworkUtil.getBundle(origin).getSymbolicName(), message, error);
  }

  public static IStatus error(Object origin, String message) {
    return error(origin.getClass(), message);
  }

  public static IStatus error(Object origin, String message, Throwable error) {
    return error(origin.getClass(), message, error);
  }

}
