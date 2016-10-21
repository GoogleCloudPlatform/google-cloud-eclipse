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

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.beans.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.ui.wizard.ServerCreationWizardPageExtension.UI_POSITION;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ServerPortExtentionTest {

  private Shell shell;
  private ServerPortExtension portExtension;

  @Before
  public void setUp() {
    // TODO(chanseok): use ShellTestResource (see AccountPanelTest) after fixing
    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/771.
    // (Remove shell.dispose() in tearDown() too.)
    shell = new Shell(Display.getDefault());
    shell.setVisible(true);
    portExtension = new ServerPortExtension();
    portExtension.createControl(UI_POSITION.BOTTOM, shell);
  }

  @After
  public void tearDown() {
    shell.dispose();
  }

  @Test
  public void testCreateControl() {
    assertNotNull(portExtension.portLabel);
    assertNotNull(portExtension.portSpinner);
    assertFalse(portExtension.portLabel.isVisible());
    assertFalse(portExtension.portSpinner.isVisible());
  }

  @Test
  public void testPortSpinnerConfiguration() {
    assertEquals(8080, portExtension.portSpinner.getSelection());
    assertEquals(1, portExtension.portSpinner.getMinimum());
    assertEquals(65535, portExtension.portSpinner.getMaximum());
  }

  @Test
  public void testHandlePropertyChanged_nullEvent() {
    portExtension.handlePropertyChanged(null);
    assertFalse(portExtension.portLabel.isVisible());
    assertFalse(portExtension.portSpinner.isVisible());
  }

  @Test
  public void testHandlePropertyChanged_nonAppEngineServerType() {
    PropertyChangeEvent nullPropertyValueEvent = new PropertyChangeEvent(new Object() /* source */,
        null /* propertyName */, null /* oldValue */, null /* newValue */);
    portExtension.handlePropertyChanged(nullPropertyValueEvent);
    assertFalse(portExtension.portLabel.isVisible());
    assertFalse(portExtension.portSpinner.isVisible());

    PropertyChangeEvent nonServerTypePropertyEvent =
        new PropertyChangeEvent(new Object(), null, null, new Object() /* not IServerType */);
    portExtension.handlePropertyChanged(nonServerTypePropertyEvent);
    assertFalse(portExtension.portLabel.isVisible());
    assertFalse(portExtension.portSpinner.isVisible());

    portExtension.handlePropertyChanged(newNonAppEngineServerTypeEvent());
    assertFalse(portExtension.portLabel.isVisible());
    assertFalse(portExtension.portSpinner.isVisible());
  }

  @Test
  public void testHandlePropertyChanged_appEngineServerType() {
    portExtension.handlePropertyChanged(newAppEngineServerTypeEvent());
    assertTrue(portExtension.portLabel.isVisible());
    assertTrue(portExtension.portSpinner.isVisible());
  }

  @Test
  public void testHandlePropertyChanged_showAndHide() {
    portExtension.handlePropertyChanged(newAppEngineServerTypeEvent());
    portExtension.handlePropertyChanged(newNonAppEngineServerTypeEvent());
    assertFalse(portExtension.portLabel.isVisible());
    assertFalse(portExtension.portSpinner.isVisible());
  }

  private static PropertyChangeEvent newAppEngineServerTypeEvent() {
    return newServerTypeChangeEvent("com.google.cloud.tools.eclipse.appengine.standard.server");
  }

  private static PropertyChangeEvent newNonAppEngineServerTypeEvent() {
    return newServerTypeChangeEvent("some.other.server");
  }

  private static PropertyChangeEvent newServerTypeChangeEvent(String serverId) {
    IServerType serverType = mock(IServerType.class);
    when(serverType.getId()).thenReturn(serverId);
    return new PropertyChangeEvent(new Object(), null, null, serverType);
  }
}
