/*******************************************************************************
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
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployPreferences;

import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

public class DeployPreferencesModelTest {

  @Test
  public void testSavePreferences_ignoreRevalidationTrickEmail() throws BackingStoreException {
    StandardDeployPreferences preferences = mock(StandardDeployPreferences.class);
    DeployPreferencesModel model = new DeployPreferencesModel(preferences);
    model.setAccountEmail(DeployPreferencesModel.REVALIDATION_TRICK_EMAIL_VALUE);

    model.savePreferences();
    verify(preferences, never()).setAccountEmail(anyString());
    verify(preferences, times(1)).setProjectId(anyString());
  }
}
