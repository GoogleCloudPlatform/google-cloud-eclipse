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

package com.google.cloud.tools.eclipse.sdk.internal;

import com.google.cloud.tools.appengine.cloudsdk.AppEngineJavaComponentsNotInstalledException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkOutOfDateException;
import com.google.common.collect.MapMaker;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;
import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;

/**
 * Provides a {@link CloudSdk} instance suitable for injection using the E4 dependency injection
 * mechanisms. As the configured location may change, we fetch the path from the context to ensure
 * that we recompute the CloudSdk instance on path change.
 */
public class CloudSdkContextFunction extends ContextFunction {
  private static final Logger logger = Logger.getLogger(CloudSdkContextFunction.class.getName());

  @SuppressWarnings("restriction")
  static final Object NOT_A_VALUE = org.eclipse.e4.core.di.IInjector.NOT_A_VALUE;

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
    Object path = context.get(PreferenceConstants.CLOUDSDK_PATH);
    if (path == null) {
      // record this context as using the preference value
      referencedContexts.add(context);
    }

    CloudSdk.Builder builder = new CloudSdk.Builder();
    Path location = toPath(path);
    if (location != null) {
      builder.sdkPath(location);
    }

    try {
      CloudSdk instance = builder.build();
      instance.validateCloudSdk();
      instance.validateAppEngineJavaComponents();
      return instance;
    } catch (CloudSdkNotFoundException | CloudSdkOutOfDateException
        | AppEngineJavaComponentsNotInstalledException ex) {
      return NOT_A_VALUE;
    }
  }

  private static Path toPath(Object path) {
    if (path instanceof File) {
      return ((File) path).toPath();
    } else if (path instanceof Path) {
      return (Path) path;
    } else if (path instanceof String) {
      return Paths.get((String) path);
    } else if (path != null) {
      logger.warning("Unsupported object for " + PreferenceConstants.CLOUDSDK_PATH + ": " + path);
    }
    return null;
  }

}
