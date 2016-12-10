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

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class LocalAppEngineServerWizardFragmentTest {
  private LocalAppEngineServerWizardFragment fragment = new LocalAppEngineServerWizardFragment();

  @Test
  public void testHasComposite() {
    Assert.assertFalse(fragment.hasComposite());
  }

  @Test
  public void testIsComplete() {
    Assert.assertTrue(fragment.isComplete());
  }
  
  @Test
  public void testCreateComposite() {
    IWizardHandle wizard = Mockito.mock(IWizardHandle.class);
    Composite parent = new Shell();
    Composite composite = fragment.createComposite(parent, wizard);
    
    Mockito.verify(wizard).setTitle("App Engine Standard Runtime");
    Mockito.verify(wizard)
        .setDescription("The App Engine Standard runtime requires the Google Cloud SDK");
    
    Control[] children = composite.getChildren();
    Assert.assertEquals(2, children.length);
    Label label = (Label) children[0];
    Assert.assertTrue(label.getText().startsWith("Cannot find the Google Cloud SDK"));
    Button button = (Button) children[1];
    Assert.assertEquals("Open the Cloud SDK Location preference page when the wizard closes",
        button.getText());
  }
}
