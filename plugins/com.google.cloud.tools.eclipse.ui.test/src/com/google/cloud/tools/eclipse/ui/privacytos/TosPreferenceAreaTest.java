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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TosPreferenceAreaTest {
  @Rule public ShellTestResource shellResource = new ShellTestResource();
  
  private Shell shell;
  private TosPreferenceArea area;
  private IPreferenceStore preferences =
      mock(IPreferenceStore.class, AdditionalAnswers.delegatesTo(new PreferenceStore()));
  @Mock private IWorkbench workbench;
  @Mock private Display display;
  
  @Before
  public void setup() {
    doReturn(display).when(workbench).getDisplay();
  }
  
  private void createPreferenceArea() {
    shell = shellResource.getShell();
    area = new TosPreferenceArea(workbench);
    area.setPreferenceStore(preferences);
    area.createContents(shell);
    area.load();
  }
  
  @Test
  public void testStatus() {
    createPreferenceArea();
    assertEquals(area.getStatus(), Status.OK_STATUS);
  }

  
}
