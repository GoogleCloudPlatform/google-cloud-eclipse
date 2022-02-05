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

import org.eclipse.equinox.log.Logger;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.log.LoggerConsumer;

/** Delegates from OSGi's newer Logger to SLF4j. */
class LoggerDelegate implements Logger {
  private org.slf4j.Logger logger;

  LoggerDelegate(org.slf4j.Logger logger) {
    this.logger = logger;
  }

  @Override
  public String getName() {
    return logger.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public void trace(String message) {
    logger.trace(message);
  }

  @Override
  public void trace(String format, Object arg) {
    logger.trace(format, arg);
  }

  @Override
  public void trace(String format, Object arg1, Object arg2) {
    logger.trace(format, arg1, arg2);
  }

  @Override
  public void trace(String format, Object... arguments) {
    logger.trace(format, arguments);
  }

  @Override
  public <E extends Exception> void trace(LoggerConsumer<E> consumer) throws E {
    if (isTraceEnabled()) {
      consumer.accept(this);
    }
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public void debug(String message) {
    logger.debug(message);
  }

  @Override
  public void debug(String format, Object arg) {
    logger.debug(format, arg);
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    logger.debug(format, arg1, arg2);
  }

  @Override
  public void debug(String format, Object... arguments) {
    logger.debug(format, arguments);
  }

  @Override
  public <E extends Exception> void debug(LoggerConsumer<E> consumer) throws E {
    if (isDebugEnabled()) {
      consumer.accept(this);
    }
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public void info(String message) {
    logger.info(message);
  }

  @Override
  public void info(String format, Object arg) {
    logger.info(format, arg);
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    logger.info(format, arg1, arg2);
  }

  @Override
  public void info(String format, Object... arguments) {
    logger.info(format, arguments);
  }

  @Override
  public <E extends Exception> void info(LoggerConsumer<E> consumer) throws E {
    if (isInfoEnabled()) {
      consumer.accept(this);
    }
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public void warn(String message) {
    logger.warn(message);
  }

  @Override
  public void warn(String format, Object arg) {
    logger.warn(format, arg);
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    logger.warn(format, arg1, arg2);
  }

  @Override
  public void warn(String format, Object... arguments) {
    logger.warn(format, arguments);
  }

  @Override
  public <E extends Exception> void warn(LoggerConsumer<E> consumer) throws E {
    if (isWarnEnabled()) {
      consumer.accept(this);
    }
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  @Override
  public void error(String message) {
    logger.error(message);
  }

  @Override
  public void error(String format, Object arg) {
    logger.error(format, arg);
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    logger.error(format, arg1, arg2);
  }

  @Override
  public void error(String format, Object... arguments) {
    logger.error(format, arguments);
  }

  @Override
  public <E extends Exception> void error(LoggerConsumer<E> consumer) throws E {
    if (isErrorEnabled()) {
      consumer.accept(this);
    }
  }

  @Override
  public void audit(String message) {
    logger.info(message);
  }

  @Override
  public void audit(String format, Object arg) {
    logger.info(format, arg);
  }

  @Override
  public void audit(String format, Object arg1, Object arg2) {
    logger.info(format, arg1, arg2);
  }

  @Override
  public void audit(String format, Object... arguments) {
    logger.info(format, arguments);
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

  @SuppressWarnings("deprecation")
  @Override
  public void log(Object context, int level, String message, Throwable exception) {
    // Oddly LogService#LOG_xxx are deprecated in favour of LogLevel enum, but
    // the LogLevel enum does not provide integer mappings.
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

  @SuppressWarnings("deprecation")
  @Override
  public boolean isLoggable(int level) {
    // Oddly LogService#LOG_xxx are deprecated in favour of LogLevel enum, but
    // the LogLevel enum does not provide integer mappings.
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
}
