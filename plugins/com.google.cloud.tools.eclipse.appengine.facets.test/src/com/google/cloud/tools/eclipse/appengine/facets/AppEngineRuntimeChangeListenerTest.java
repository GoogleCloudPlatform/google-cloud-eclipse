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

package com.google.cloud.tools.eclipse.appengine.facets;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IPrimaryRuntimeChangedEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineRuntimeChangeListenerTest {
  private AppEngineStandardRuntimeChangeListener listener = new AppEngineStandardRuntimeChangeListener();
  @Mock private IPrimaryRuntimeChangedEvent event;

  
  /**
   * Tests that the project is not retrieved if the event type is not 
   * <code>IFacetedProjectEvent.Type.PRIMARY_RUNTIME_CHANGED</code>
   */
  @Test
  public void testHandleEvent_notPrimaryRuntimeChangeEvent() {
    when(event.getType()).thenReturn(IFacetedProjectEvent.Type.TARGETED_RUNTIMES_CHANGED);
    listener.handleEvent(event);
    verify(event, never()).getNewPrimaryRuntime();
    verify(event, never()).getProject();
  }

  /**
   * Tests that the primary runtime of the event is retrieved if the event type
   * is <code>IFacetedProjectEvent.Type.PRIMARY_RUNTIME_CHANGED</code>
   */
  @Test
  public void testHandleEvent_primaryRuntimeChangeEvent() {
    when(event.getType()).thenReturn(IFacetedProjectEvent.Type.PRIMARY_RUNTIME_CHANGED);
    listener.handleEvent(event);
    verify(event, atLeast(1)).getNewPrimaryRuntime();
  }
  
}
