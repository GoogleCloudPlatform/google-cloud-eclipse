package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import static org.junit.Assert.*;

import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.junit.Rule;
import org.junit.Test;

public class BlankDeployPreferencesPanelTest {

  @Rule public ShellTestResource shellTestResource = new ShellTestResource();

  @Test
  public void testGetHelpContextId() {
    assertNull(new BlankDeployPreferencesPanel(shellTestResource.getShell()).getHelpContextId());
  }
}
