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
import org.osgi.service.log.LoggerFactory;

/**
 * An SLF4j facade for OSGi {@link LoggerFactory} and Eclipse {@link ExtendedLogService}. This class
 * is intended to be a replacement for other ExtendedLogService implementations.
 */
public class EclipseLoggingAdapter extends LoggerDelegate implements ExtendedLogService {

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
