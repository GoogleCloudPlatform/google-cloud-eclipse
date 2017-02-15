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

package com.google.cloud.tools.eclipse.util.service;

import javax.inject.Inject;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.core.contexts.IEclipseContext;

/**
 * Used to test E4 Dependency Injection in {@link ServiceContextFactoryTest}.
 */
public class TestInjectionTarget {

  @Inject
  private IEclipseContext eclipseContext;
  @Inject
  private IExtensionRegistry extensionRegistry;

  public IEclipseContext getEclipseContext() {
    return eclipseContext;
  }

  public IExtensionRegistry getExtensionRegistry() {
    return extensionRegistry;
  }
}
