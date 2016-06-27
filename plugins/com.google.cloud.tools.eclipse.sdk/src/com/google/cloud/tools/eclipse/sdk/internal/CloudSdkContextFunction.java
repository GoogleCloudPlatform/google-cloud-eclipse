/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.eclipse.sdk.internal;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.sdk.CloudSdkProvider;
import com.google.common.collect.MapMaker;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.IInjector;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Provides a {@link CloudSdk} instance suitable for injection using the E4 dependency injection
 * mechanisms. As the configured location may change, we fetch the path from the context to ensure
 * that we recompute the CloudSdk instance on path change.
 */
public class CloudSdkContextFunction extends ContextFunction {
  private static final Logger logger = Logger.getLogger(CloudSdkContextFunction.class.getName());

  @SuppressWarnings("restriction")
  static final Object NOT_A_VALUE = IInjector.NOT_A_VALUE;

  /**
   * A list of referenced IEclipseContexts that must be updated on preference change.
   */
  private static final Set<IEclipseContext> referencedContexts =
      Collections.newSetFromMap(new MapMaker().weakKeys().<IEclipseContext, Boolean>makeMap());

  /** Cloud SDK location has been changed: trigger any necessary updates. */
  static void sdkPathChanged(String newPath) {
    for (IEclipseContext context : referencedContexts) {
      context.set(PreferenceConstants.CLOUDSDK_PATH, newPath);
    }
  }

  @Override
  public Object compute(IEclipseContext context, String contextKey) {
    referencedContexts.add(context);

    Object path = context.get(PreferenceConstants.CLOUDSDK_PATH);
    File location = toFile(path);
    CloudSdk.Builder builder = new CloudSdkProvider(null).createBuilder(location);
    if (builder == null) {
      return NOT_A_VALUE;
    }
    CloudSdk instance = builder.build();
    try {
      instance.validate();
      return instance;
    } catch (AppEngineException ex) {
      return NOT_A_VALUE;
    }
  }

  private static File toFile(Object path) {
    if (path instanceof File) {
      return (File) path;
    } else if (path instanceof Path) {
      return ((Path) path).toFile();
    } else if (path instanceof String) {
      return new File((String) path);
    } else if (path != null) {
      logger.warning("Unsupported object for " + PreferenceConstants.CLOUDSDK_PATH + ": " + path);
    }
    return null;
  }

}
