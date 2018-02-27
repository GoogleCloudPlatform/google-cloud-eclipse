/*
 * Copyright 2018 Google Inc.
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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.cloud.tools.managedcloudsdk.ProgressListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test {@link ProgressWrapper}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProgressWrapperTest {
  @Mock IProgressMonitor monitor;

  @Test
  public void testLifecycle() {
    // SubMonitor may issue calls with slightly different numbers and in different orders.
    ArgumentCaptor<Integer> totalWorkCaptor = ArgumentCaptor.forClass(Integer.class);

    ProgressWrapper wrapper = new ProgressWrapper(monitor);
    wrapper.start("message", 100);
    wrapper.update("update");
    wrapper.update(100);
    wrapper.done();
    
    verify(monitor).beginTask(eq(""), totalWorkCaptor.capture());
    verify(monitor).setTaskName("message");
    verify(monitor).subTask("update");
    verify(monitor).worked(totalWorkCaptor.getValue());
    verify(monitor).done();
    verifyNoMoreInteractions(monitor);
  }

  @Test
  public void testNewChild() {
    ProgressWrapper wrapper = new ProgressWrapper(monitor);
    wrapper.start("testNewChild", 100);
    assertThat(wrapper.newChild(10), instanceOf(ProgressListener.class));
  }
}
