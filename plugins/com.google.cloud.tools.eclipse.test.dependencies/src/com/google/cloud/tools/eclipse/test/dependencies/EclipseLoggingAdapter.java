/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.test.dependencies;

import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.log.LoggerFactory;

/** An SLF4j facade for OSGi LoggerFactory and Eclipse ExtendedLogService */
public class EclipseLoggingAdapter extends LoggerDelegate
    implements LoggerFactory, ExtendedLogService {

  public EclipseLoggingAdapter() {
    super(org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME));
  }

  @Override
  public Logger getLogger(String name) {
    return new LoggerDelegate(org.slf4j.LoggerFactory.getLogger(name));
  }

  @Override
  public Logger getLogger(Class<?> clazz) {
    return new LoggerDelegate(org.slf4j.LoggerFactory.getLogger(clazz));
  }

  @Override
  public Logger getLogger(Bundle bundle, String loggerName) {
    return new LoggerDelegate(org.slf4j.LoggerFactory.getLogger(bundle.getSymbolicName()));
  }

  @Override
  public void log(int level, String message) {
    log(level, message, null);
  }

  @Override
  public void log(int level, String message, Throwable exception) {
    log(null, level, message, exception);
  }

  @Override
  public void log(ServiceReference<?> sr, int level, String message) {
    log(level, message);
  }

  @Override
  public void log(ServiceReference<?> sr, int level, String message, Throwable exception) {
    log(level, message, exception);
  }

  @Override
  public void log(Object context, int level, String message) {
    log(context, level, message, null);
  }

  private Logger getLogger(Object context) {
    if (context instanceof Class) {
      return getLogger((Class) context);
    } else if (context != null) {
      return getLogger(context.getClass());
    }
    return this;
  }

  @Override
  public void log(Object context, int level, String message, Throwable exception) {
    Logger logger = getLogger(context);
    switch (level) {
      case LogService.LOG_ERROR:
        logger.error(message, exception);
        break;
      case LogService.LOG_WARNING:
        logger.warn(message, exception);
        break;
      case LogService.LOG_INFO:
        logger.info(message, exception);
        break;
      case LogService.LOG_DEBUG:
        logger.debug(message, exception);
        break;
      default:
        logger.debug(message, exception);
        break;
    }
  }

  @Override
  public boolean isLoggable(int level) {
    switch (level) {
      case LogService.LOG_ERROR:
        return isErrorEnabled();
      case LogService.LOG_WARNING:
        return isWarnEnabled();
      case LogService.LOG_INFO:
        return isInfoEnabled();
      case LogService.LOG_DEBUG:
        return isDebugEnabled();
      default:
        return false;
    }
  }

  @Override
  public <L extends org.osgi.service.log.Logger> L getLogger(String name, Class<L> loggerType) {
    if (loggerType == org.osgi.service.log.Logger.class) {
      return loggerType.cast(getLogger(name));
    }
    return null;
  }

  @Override
  public <L extends org.osgi.service.log.Logger> L getLogger(Class<?> clazz, Class<L> loggerType) {
    if (loggerType == org.osgi.service.log.Logger.class) {
      return loggerType.cast(getLogger(clazz));
    }
    return null;
  }

  @Override
  public <L extends org.osgi.service.log.Logger> L getLogger(
      Bundle bundle, String name, Class<L> loggerType) {
    if (loggerType == org.osgi.service.log.Logger.class) {
      return loggerType.cast(getLogger(bundle, name));
    }
    return null;
  }
}
