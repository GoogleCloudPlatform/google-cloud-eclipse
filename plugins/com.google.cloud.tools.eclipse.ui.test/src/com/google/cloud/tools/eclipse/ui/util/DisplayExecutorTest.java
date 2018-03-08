/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.ui.util;

import static org.mockito.Mockito.verify;

import org.eclipse.swt.widgets.Display;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class DisplayExecutorTest {
  @Mock private Display display;
  private DisplayExecutor executor;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    executor = DisplayExecutor.create(display);
  }

  @Test
  public void testExecute() {
    Runnable runnable = () -> { /* no nothing */ };
    executor.execute(runnable);
    verify(display).asyncExec(runnable);
  }
}
