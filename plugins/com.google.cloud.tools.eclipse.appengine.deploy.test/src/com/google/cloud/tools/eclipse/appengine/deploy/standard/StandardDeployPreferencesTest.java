package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import static org.junit.Assert.*;

import static org.hamcrest.text.IsEmptyString.isEmptyString;
import org.junit.Test;

public class StandardDeployPreferencesTest {

  @Test
  public void testDefaultPromptProjectId() {
    assertTrue(StandardDeployPreferences.DEFAULT.isPromptForProjectId());
  }

  @Test
  public void testDefaultProjectId() {
    assertThat(StandardDeployPreferences.DEFAULT.getProjectId(), isEmptyString());
  }

  @Test
  public void testDefaultOverrideDefaultVersioning() {
    assertFalse(StandardDeployPreferences.DEFAULT.isOverrideDefaultVersioning());
  }

  @Test
  public void testDefaultVersion() {
    assertThat(StandardDeployPreferences.DEFAULT.getVersion(), isEmptyString());
  }

  @Test
  public void testDefaultAutoPromote() {
    assertTrue(StandardDeployPreferences.DEFAULT.isAutoPromote());
  }

  @Test
  public void testDefaultOverrideDefaultBucket() {
    assertTrue(StandardDeployPreferences.DEFAULT.isOverrideDefaultBucket());
  }

  @Test
  public void testDefaultBucket() {
    assertThat(StandardDeployPreferences.DEFAULT.getBucket(), isEmptyString());
  }

}
