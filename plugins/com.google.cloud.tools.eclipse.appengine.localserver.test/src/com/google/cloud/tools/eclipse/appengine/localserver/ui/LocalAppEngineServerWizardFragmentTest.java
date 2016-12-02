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

import org.junit.Assert;
import org.junit.Test;

public class LocalAppEngineServerWizardFragmentTest {
  private LocalAppEngineServerWizardFragment wizardFragment;

  @Test
  public void testHasComposite_cloudSdkExists() {
    wizardFragment = new LocalAppEngineServerWizardFragment(true);
    Assert.assertFalse(wizardFragment.hasComposite());
  }

  @Test
  public void testHasComposite_cloudSdkDoesNotExists() {
    wizardFragment = new LocalAppEngineServerWizardFragment(false);
    Assert.assertTrue(wizardFragment.hasComposite());
  }

  @Test
  public void testIsComplete_cloudSdkExists() {
    wizardFragment = new LocalAppEngineServerWizardFragment(true);
    Assert.assertTrue(wizardFragment.isComplete());
  }

  @Test
  public void testIsComplete_cloudSdkDoesNotExists() {
    wizardFragment = new LocalAppEngineServerWizardFragment(false);
    Assert.assertFalse(wizardFragment.isComplete());

    wizardFragment.enter();
    Assert.assertTrue(wizardFragment.isComplete());
  }
}
