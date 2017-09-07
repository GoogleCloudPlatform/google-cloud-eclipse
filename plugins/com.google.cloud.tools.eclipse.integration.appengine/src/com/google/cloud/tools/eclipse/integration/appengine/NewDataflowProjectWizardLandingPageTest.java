/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.eclipse.integration.appengine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowProjectCreator;
import com.google.cloud.tools.eclipse.dataflow.ui.page.NewDataflowProjectWizardLandingPage;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NewDataflowProjectWizardLandingPageTest {

  private Display display;
  private Shell shell;

  private NewDataflowProjectWizardLandingPage page;
  private Combo templateDropdown;
  private Combo templateVersionDropdown;

  @Before
  public void setUp() {
    display = Display.getDefault();
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        shell = new Shell(display);
        page = new NewDataflowProjectWizardLandingPage(mock(DataflowProjectCreator.class));
        page.createControl(shell);

        templateDropdown =
            CompositeUtil.findControlAfterLabel(shell, Combo.class, "Project &template:");
        templateVersionDropdown =
            CompositeUtil.findControlAfterLabel(shell, Combo.class, "Dataflow &version:");
      }
    });
  }

  @After
  public void tearDown() {
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        shell.dispose();
      }
    });
  }

  @Test
  public void testTemplateVersionsDropdown_starterTemplate() {
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        assertEquals(0, templateDropdown.getSelectionIndex());
        assertEquals("Starter project with a simple pipeline", templateDropdown.getText());
        assertArrayEquals(new String[] {"2.0.0", "1.9.0"}, templateVersionDropdown.getItems());
      }
    });
  }

  @Test
  public void testTemplateVersionsDropdown_exampleTemplate() {
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        templateDropdown.select(1);
        assertEquals("Example pipelines", templateDropdown.getText());
        assertArrayEquals(new String[] {"2.0.0", "1.9.0"}, templateVersionDropdown.getItems());
      }
    });
  }
}
