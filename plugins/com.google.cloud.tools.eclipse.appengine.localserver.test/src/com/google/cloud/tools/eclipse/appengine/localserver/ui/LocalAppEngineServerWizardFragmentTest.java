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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class LocalAppEngineServerWizardFragmentTest {
  private LocalAppEngineServerWizardFragment wizardFragment;

  @Test
  public void testHasComposite_cloudSdkExists() {
    wizardFragment = new LocalAppEngineServerWizardFragment();
    Assert.assertTrue(wizardFragment.hasComposite());
  }

  @Test
  public void testHasComposite_cloudSdkDoesNotExist() {
    wizardFragment = new LocalAppEngineServerWizardFragment(null);
    Assert.assertTrue(wizardFragment.hasComposite());
  }

  @Test
  public void testIsComplete_cloudSdkExists() {
    wizardFragment = new LocalAppEngineServerWizardFragment();
    Assert.assertTrue(wizardFragment.isComplete());
  }

  @Test
  public void testIsComplete_cloudSdkDoesNotExist() {
    wizardFragment = new LocalAppEngineServerWizardFragment(null);
    Assert.assertFalse(wizardFragment.isComplete());

    wizardFragment.enter();
    Assert.assertTrue(wizardFragment.isComplete());
  }
  
  @Test
  public void testCreateComposite() {
    LocalAppEngineServerWizardFragment wizardFragment = new LocalAppEngineServerWizardFragment();
    IWizardHandle wizard = Mockito.mock(IWizardHandle.class);
    Composite parent = new Shell();
    Composite composite = wizardFragment.createComposite(parent, wizard);
    
    Mockito.verify(wizard).setTitle("App Engine Standard Runtime");
    Mockito.verify(wizard)
        .setDescription("The App Engine Standard runtime requires the Google Cloud SDK");
    
    Assert.assertEquals(1, composite.getChildren().length);
    Label label = (Label) composite.getChildren()[0];
    Assert.assertTrue(label.getText().startsWith("Using the Google Cloud SDK installed in "));
  }
}
