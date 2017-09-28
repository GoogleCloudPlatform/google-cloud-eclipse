/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.dataflow.ui.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.ui.page.MessageTarget;
import com.google.cloud.tools.eclipse.dataflow.ui.preferences.RunOptionsDefaultsComponent;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * DefaultedPipelineOptionsComponent is responsible for saving and restoring custom values when the
 * user toggles the <em>use defaults</em> button.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultedPipelineOptionsComponentTest {
  @Rule
  public ShellTestResource shellCreator = new ShellTestResource();
  @Mock
  private MessageTarget messageTarget;

  @Mock
  private DataflowPreferences preferences;
  private RunOptionsDefaultsComponent defaultOptions;
  private String accountEmail;
  private String projectId;
  private String stagingLocation;

  @Before
  public void setUp() {
    // This is easier than subclassing RunOptionsDefaultComponent
    Answer<String> answerAccountEmail = new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return Strings.nullToEmpty(
            accountEmail == null ? preferences.getDefaultAccountEmail() : accountEmail);
      }
    };
    Answer<Void> recordAccountEmail = new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        accountEmail = invocation.getArgumentAt(0, String.class);
        return null;
      }
    };

    Answer<String> answerProjectId = new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return Strings.nullToEmpty(projectId == null ? preferences.getDefaultProject() : projectId);
      }
    };
    Answer<Void> recordProjectId = new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        projectId = invocation.getArgumentAt(0, String.class);
        return null;
      }
    };
    Answer<String> answerStagingLocation = new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return Strings.nullToEmpty(
            stagingLocation == null ? preferences.getDefaultStagingLocation() : stagingLocation);
      }
    };
    Answer<Void> recordStagingLocation = new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        stagingLocation = invocation.getArgumentAt(0, String.class);
        return null;
      }
    };

    defaultOptions = mock(RunOptionsDefaultsComponent.class);
    doAnswer(answerAccountEmail).when(defaultOptions).getAccountEmail();
    doAnswer(recordAccountEmail).when(defaultOptions).selectAccount(anyString());
    doThrow(IllegalStateException.class).when(defaultOptions).getProject();
    doAnswer(answerProjectId).when(defaultOptions).getProjectId();
    doAnswer(recordProjectId).when(defaultOptions).setCloudProjectText(anyString());
    doAnswer(answerStagingLocation).when(defaultOptions).getStagingLocation();
    doAnswer(recordStagingLocation).when(defaultOptions).setStagingLocationText(anyString());
  }

  @Test
  public void testDefaults_nulls() {
    DefaultedPipelineOptionsComponent component = new DefaultedPipelineOptionsComponent(
        shellCreator.getShell(), null, messageTarget, preferences, defaultOptions);

    assertTrue(component.isUseDefaultOptions());
    Map<String, String> values = component.getValues();
    // all empty/null settings should be turned to empty strings
    assertEquals("", values.get(DataflowPreferences.ACCOUNT_EMAIL_PROPERTY));
    assertEquals("", values.get(DataflowPreferences.PROJECT_PROPERTY));
    assertEquals("", values.get(DataflowPreferences.STAGING_LOCATION_PROPERTY));
    assertEquals("", values.get(DataflowPreferences.GCP_TEMP_LOCATION_PROPERTY));
  }

  @Test
  public void testDefaults_values() {
    doReturn("pref-email").when(preferences).getDefaultAccountEmail();
    doReturn("pref-project").when(preferences).getDefaultProject();
    doReturn("gs://pref-staging").when(preferences).getDefaultStagingLocation();
    doReturn("gs://pref-staging").when(preferences).getDefaultGcpTempLocation();

    DefaultedPipelineOptionsComponent component = new DefaultedPipelineOptionsComponent(
        shellCreator.getShell(), null, messageTarget, preferences, defaultOptions);

    assertTrue(component.isUseDefaultOptions());
    Map<String, String> values = component.getValues();
    assertEquals("pref-email", values.get(DataflowPreferences.ACCOUNT_EMAIL_PROPERTY));
    assertEquals("pref-project", values.get(DataflowPreferences.PROJECT_PROPERTY));
    assertEquals("gs://pref-staging", values.get(DataflowPreferences.STAGING_LOCATION_PROPERTY));
    assertEquals("gs://pref-staging", values.get(DataflowPreferences.GCP_TEMP_LOCATION_PROPERTY));
  }

  @Test
  public void testCustomValues_nulls() {
    doReturn("pref-email").when(preferences).getDefaultAccountEmail();
    doReturn("pref-project").when(preferences).getDefaultProject();
    doReturn("gs://pref-staging").when(preferences).getDefaultStagingLocation();
    doReturn("gs://pref-staging").when(preferences).getDefaultGcpTempLocation();

    DefaultedPipelineOptionsComponent component = new DefaultedPipelineOptionsComponent(
        shellCreator.getShell(), null, messageTarget, preferences, defaultOptions);
    component.setCustomValues(new HashMap<String, String>());
    component.setUseDefaultValues(false);

    // the empty/null customValues should override the preferences
    Map<String, String> values = component.getValues();
    assertEquals("", values.get(DataflowPreferences.ACCOUNT_EMAIL_PROPERTY));
    assertEquals("", values.get(DataflowPreferences.PROJECT_PROPERTY));
    assertEquals("", values.get(DataflowPreferences.STAGING_LOCATION_PROPERTY));
    assertEquals("", values.get(DataflowPreferences.GCP_TEMP_LOCATION_PROPERTY));
  }


  @Test
  public void testCustomValues_values() {
    DefaultedPipelineOptionsComponent component = new DefaultedPipelineOptionsComponent(
        shellCreator.getShell(), null, messageTarget, preferences, defaultOptions);

    // setting useDefaultValues=false should store current values and restore any custom values
    Map<String, String> customValues = new HashMap<>();
    customValues.put(DataflowPreferences.ACCOUNT_EMAIL_PROPERTY, "newEmail");
    customValues.put(DataflowPreferences.PROJECT_PROPERTY, "newProject");
    customValues.put(DataflowPreferences.STAGING_LOCATION_PROPERTY, "newStagingLocation");
    customValues.put(DataflowPreferences.GCP_TEMP_LOCATION_PROPERTY, "newTempLocation");

    component.setCustomValues(customValues);
    component.setUseDefaultValues(false);

    Map<String, String> values = component.getValues();
    assertEquals("newEmail", values.get(DataflowPreferences.ACCOUNT_EMAIL_PROPERTY));
    assertEquals("newProject", values.get(DataflowPreferences.PROJECT_PROPERTY));
    assertEquals("newStagingLocation", values.get(DataflowPreferences.STAGING_LOCATION_PROPERTY));
    assertEquals("newStagingLocation", values.get(DataflowPreferences.GCP_TEMP_LOCATION_PROPERTY));
  }

  @Test
  public void testToggleDefaults() {
    doReturn("pref-email").when(preferences).getDefaultAccountEmail();
    doReturn("pref-project").when(preferences).getDefaultProject();
    doReturn("gs://pref-staging").when(preferences).getDefaultStagingLocation();
    doReturn("gs://pref-staging").when(preferences).getDefaultGcpTempLocation();

    Map<String, String> customValues = new HashMap<>();
    customValues.put(DataflowPreferences.ACCOUNT_EMAIL_PROPERTY, "newEmail");
    customValues.put(DataflowPreferences.PROJECT_PROPERTY, "newProject");
    customValues.put(DataflowPreferences.STAGING_LOCATION_PROPERTY, "newStagingLocation");
    customValues.put(DataflowPreferences.GCP_TEMP_LOCATION_PROPERTY, "newTempLocation");

    DefaultedPipelineOptionsComponent component = new DefaultedPipelineOptionsComponent(
        shellCreator.getShell(), null, messageTarget, preferences, defaultOptions);

    assertTrue(component.isUseDefaultOptions());
    component.setCustomValues(customValues);
    // preferences should be returned, despite customValues being set
    Map<String, String> values = component.getValues();
    assertEquals("pref-email", values.get(DataflowPreferences.ACCOUNT_EMAIL_PROPERTY));
    assertEquals("pref-project", values.get(DataflowPreferences.PROJECT_PROPERTY));
    assertEquals("gs://pref-staging", values.get(DataflowPreferences.STAGING_LOCATION_PROPERTY));
    assertEquals("gs://pref-staging", values.get(DataflowPreferences.GCP_TEMP_LOCATION_PROPERTY));

    // setting useDefaultValues=false should store current values and restore any custom values
    component.setUseDefaultValues(false);
    values = component.getValues();
    assertEquals("newEmail", values.get(DataflowPreferences.ACCOUNT_EMAIL_PROPERTY));
    assertEquals("newProject", values.get(DataflowPreferences.PROJECT_PROPERTY));
    assertEquals("newStagingLocation", values.get(DataflowPreferences.STAGING_LOCATION_PROPERTY));
    assertEquals("newStagingLocation", values.get(DataflowPreferences.GCP_TEMP_LOCATION_PROPERTY));

    // setting useDefaultValues=true should restore the preferences values
    component.setUseDefaultValues(true);
    values = component.getValues();
    assertEquals("pref-email", values.get(DataflowPreferences.ACCOUNT_EMAIL_PROPERTY));
    assertEquals("pref-project", values.get(DataflowPreferences.PROJECT_PROPERTY));
    assertEquals("gs://pref-staging", values.get(DataflowPreferences.STAGING_LOCATION_PROPERTY));
    assertEquals("gs://pref-staging", values.get(DataflowPreferences.GCP_TEMP_LOCATION_PROPERTY));

    // setting useDefaultValues=false should store current values and restore any custom values
    component.setUseDefaultValues(false);
    values = component.getValues();
    assertEquals("newEmail", values.get(DataflowPreferences.ACCOUNT_EMAIL_PROPERTY));
    assertEquals("newProject", values.get(DataflowPreferences.PROJECT_PROPERTY));
    assertEquals("newStagingLocation", values.get(DataflowPreferences.STAGING_LOCATION_PROPERTY));
    assertEquals("newStagingLocation", values.get(DataflowPreferences.GCP_TEMP_LOCATION_PROPERTY));
  }
}
