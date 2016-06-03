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

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.IInjector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Provides an {@link CloudSdk} instance suitable for injection using the E4 DI mechanisms. As the
 * configured location may change, we fetch the path from the context to ensure that we recompute
 * the CloudSdk instance on path change.
 */
public class CloudSdkContextFunction extends ContextFunction {
  /**
   * A list of IEclipseContexts that have been referenced that need to be updated on preference
   * change
   */
  private static final Map<IEclipseContext, IEclipseContext> referencedContexts =
      Collections.synchronizedMap(new WeakHashMap<IEclipseContext, IEclipseContext>());

  /** Cloud SDK location has been changed: trigger any necessary updates */
  public static void sdkPathChanged(Object newPath) {
    List<IEclipseContext> contexts;
    synchronized (referencedContexts) {
      contexts = new ArrayList<>(referencedContexts.keySet());
    }
    for (IEclipseContext c : contexts) {
      c.set(PreferenceConstants.CLOUDSDK_PATH, newPath);
    }
  }

  @Override
  public Object compute(IEclipseContext context, String contextKey) {
    Object path = context.get(PreferenceConstants.CLOUDSDK_PATH);
    referencedContexts.put(context, context);

    CloudSdk.Builder builder = CloudSdkProvider.createBuilder(path);
    if (builder == null) {
      return IInjector.NOT_A_VALUE;
    }
    CloudSdk instance = builder.build();
    try {
      instance.validate();
      return instance;
    } catch (AppEngineException e) {
      return IInjector.NOT_A_VALUE;
    }
  }

}
