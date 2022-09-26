/*
 * Copyright 2022 Google LLC
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

package com.google.cloud.tools.eclipse.ui.privacytos;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.eclipse.core.runtime.Status;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TosPreferenceAreaTest {
  @Mock private TosPreferenceArea tosPreferenceArea;
  
  @Test
  public void testStatus() {
    assertEquals(tosPreferenceArea.getStatus(), Status.OK_STATUS);
  }

  public void testLoad() {
    verify(tosPreferenceArea).load();
  }

  public void loadDefault() {
    verify(tosPreferenceArea).loadDefault();
  }

  public void performApply() {
    verify(tosPreferenceArea, never()).performApply();
  }
  
}
