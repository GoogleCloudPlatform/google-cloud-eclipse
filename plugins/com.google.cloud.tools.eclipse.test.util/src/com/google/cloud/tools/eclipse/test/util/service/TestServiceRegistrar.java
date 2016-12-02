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

package com.google.cloud.tools.eclipse.test.util.service;

import static org.mockito.Mockito.mock;

import java.util.Hashtable;
import org.junit.rules.ExternalResource;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public class TestServiceRegistrar<S> extends ExternalResource {

  private ServiceRegistration<S> serviceRegistration;
  private Class<S> serviceClass;
  private S service;

  public TestServiceRegistrar(Class<S> serviceClass) {
    this.serviceClass = serviceClass;
  }

  @Override
  protected void before() throws Throwable {
    service = mock(serviceClass);
    registerMockService();
  }

  @Override
  protected void after() {
    if (serviceRegistration != null) {
      serviceRegistration.unregister();
    }
  }

  private void registerMockService() {
    Hashtable<String, Object> properties = new Hashtable<>();
    properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
    serviceRegistration = FrameworkUtil.getBundle(getClass()).getBundleContext()
        .registerService(serviceClass, service, properties);
  }

  public S getRepositoryService() {
    return service;
  }
}
