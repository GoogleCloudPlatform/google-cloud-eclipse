/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import static org.mockito.Matchers.any;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsArrayContainingInOrder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests of {@link ModuleUtils#getAllModules(org.eclipse.wst.server.core.IServer)}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ModuleUtilsGetAllModulesTest {
  @Mock
  IServer server;

  @Mock(name = "module1")
  private IModule module1;
  @Mock(name = "module2a")
  private IModule module2a;
  @Mock(name = "module2b")
  private IModule module2b;
  @Mock(name = "module3")
  private IModule module3;



  @Test
  public void testGetAllModules_single() {
    Mockito.when(server.getModules()).thenReturn(new IModule[] {module1});
    Mockito.when(server.getChildModules(any(IModule[].class), any(IProgressMonitor.class)))
        .thenReturn(new IModule[0]);

    IModule[] result = ModuleUtils.getAllModules(server);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.length);
    Assert.assertThat(result[0], Matchers.sameInstance(module1));
  }

  @Test
  public void testGetAllModules_multi() {
    Mockito.when(server.getModules()).thenReturn(new IModule[] {module1});
    Mockito.when(server.getChildModules(any(IModule[].class), any(IProgressMonitor.class)))
        .thenReturn(new IModule[0]);

    Mockito.when(server.getChildModules(AdditionalMatchers.aryEq(new IModule[] {module1}),
        any(IProgressMonitor.class))).thenReturn(new IModule[] {module2a, module2b});

    Mockito.when(server.getChildModules(AdditionalMatchers.aryEq(new IModule[] {module1, module2b}),
        any(IProgressMonitor.class))).thenReturn(new IModule[] {module3});

    IModule[] result = ModuleUtils.getAllModules(server);
    Assert.assertNotNull(result);
    Assert.assertEquals(4, result.length);
    Assert.assertThat(result,
        IsArrayContainingInOrder.arrayContaining(module1, module2a, module2b, module3));
  }
}
