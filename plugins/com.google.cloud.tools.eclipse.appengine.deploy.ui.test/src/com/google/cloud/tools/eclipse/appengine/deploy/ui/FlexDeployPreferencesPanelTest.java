package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexDeployPreferences;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Shell;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FlexDeployPreferencesPanelTest {

  @Rule public ShellTestResource shellTestResource = new ShellTestResource();
  @Mock private IProject project;
  @Mock private FlexDeployPreferences preferences;

  @Test
  public void testGetHelpContextId() throws Exception {
    when(preferences.getUseDeploymentPreferences()).thenReturn(false);
    when(preferences.getDockerDirectory()).thenReturn("/non/existent/docker/directory");
    Shell parent = shellTestResource.getShell();
    assertThat(new FlexDeployPreferencesPanel(parent, project, preferences).getHelpContextId(),
        is("com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployAppEngineFlexProjectContext"));
  }
}
