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

package com.google.cloud.tools.eclipse.appengine.localserver;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

/**
 * Helper class to be used with {@link Rule} to make tests easier that depend on {@link IEclipseContext}. It provides
 * an empty context that can be filled with object needed for the test using the {@link #set(Class, Object)} method.
 * <p>
 * Creates a new context instance for each test runs and takes care of the disposal of the context object after
 * the test has been executed.
 * <p>
 * Mocking the {@link IEclipseContext} objects can be complicated hence this class. mock(IEclipseContext.class) causes
 * ClassCastException, mock(EclipseContext.class) throws an error during service lookup.
 */
public class EclipseContextHolder extends ExternalResource {

  private IEclipseContext eclipseContext;

  @Override
  protected void before() throws Throwable {
    eclipseContext = EclipseContextFactory.create();
  }

  @Override
  protected void after() {
    if (eclipseContext != null) {
      eclipseContext.dispose();
    }
  }

  /**
   * Calls {@link IEclipseContext#set(Class, Object)} on the underlying context object.
   */
  public <T> void set(Class<T> clazz, T value) {
    eclipseContext.set(clazz, value);
  }

  /**
   * @return the underlying {@link IEclipseContext} instance 
   */
  public IEclipseContext getContext() {
    return eclipseContext;
  }
}
