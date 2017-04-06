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

package com.google.cloud.tools.eclipse.dataflow.ui.preferences;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IWorkbench;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link GoogleLandingPage}.
 */
@RunWith(JUnit4.class)
public class GoogleLandingPageTest {
  @Test
  public void getElementAfterSetElement() {
    GoogleLandingPage page = new GoogleLandingPage();
    IAdaptable adaptable = mock(IAdaptable.class);

    page.setElement(adaptable);
    assertEquals(adaptable, page.getElement());
  }

  @Test
  public void initSucceeds() {
    GoogleLandingPage page = new GoogleLandingPage();
    page.init(mock(IWorkbench.class));
  }
}
