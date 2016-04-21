/*******************************************************************************
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.google.cloud.tools.eclipse.appengine.localserver.Activator;
import com.google.cloud.tools.eclipse.appengine.localserver.server.CloudSdkServer;

/**
 * A launch configuration tab that displays and edits "dev_appserver.py" command
 * flags for the Cloud SDK server.
 */
public class CloudSdkServerLaunchTab extends AbstractLaunchConfigurationTab {
  protected Text programFlagText;

  @Override
  public void createControl(Composite parent) {
    Font font = parent.getFont();
    Composite comp = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(1, true);
    comp.setLayout(layout);
    comp.setFont(font);

    GridData gridData = new GridData(GridData.FILL_BOTH);
    comp.setLayoutData(gridData);
    setControl(comp);

    Group group = new Group(comp, SWT.NONE);
    group.setFont(font);
    layout = new GridLayout();
    group.setLayout(layout);
    group.setLayoutData(new GridData(GridData.FILL_BOTH));

    String controlName = ("Program &flags");
    group.setText(controlName);

    programFlagText = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
    programFlagText.addTraverseListener(new TraverseListener() {
      @Override
      public void keyTraversed(TraverseEvent e) {
        switch (e.detail) {
        case SWT.TRAVERSE_ESCAPE:
        case SWT.TRAVERSE_PAGE_NEXT:
        case SWT.TRAVERSE_PAGE_PREVIOUS:
          e.doit = true;
          break;
        case SWT.TRAVERSE_RETURN:
        case SWT.TRAVERSE_TAB_NEXT:
        case SWT.TRAVERSE_TAB_PREVIOUS:
          if ((programFlagText.getStyle() & SWT.SINGLE) != 0) {
            e.doit = true;
          } else {
            if (!programFlagText.isEnabled() || (e.stateMask & SWT.MODIFIER_MASK) != 0) {
              e.doit = true;
            }
          }
          break;
        }
      }
    });
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.heightHint = 40;
    gridData.widthHint = 100;
    programFlagText.setLayoutData(gridData);
    programFlagText.setFont(font);
    programFlagText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent evt) {
        scheduleUpdateJob();
      }
    });

    String buttonLabel = "Var&iables...";
    Button programFlagVariableButton = createPushButton(group, buttonLabel, null);
    programFlagVariableButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    programFlagVariableButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        ServerFlagSelectionDialog dialog = new ServerFlagSelectionDialog(getShell());
        dialog.open();
        String variable = dialog.getVariableExpression();
        programFlagText.insert(variable);
      }
    });
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    configuration.setAttribute(CloudSdkServer.SERVER_PROGRAM_FLAGS, (String) null);
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    try {
      programFlagText.setText(configuration.getAttribute(CloudSdkServer.SERVER_PROGRAM_FLAGS, ""));
    } catch (CoreException e) {
      setErrorMessage("Exception occurred reading configuration:" + e.getStatus().getMessage());
      Activator.logError(e);
    }
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    configuration.setAttribute(CloudSdkServer.SERVER_PROGRAM_FLAGS,
                               getAttributeValueFrom(programFlagText));
  }

  @Override
  public String getName() {
    return "App Engine Dev Server Flags";
  }

  /**
   * Returns the string in the text widget, or {@code null} if empty.
   *
   * @param text the widget to get the value from
   * @return text or {@code null}
   */
  protected String getAttributeValueFrom(Text text) {
    String content = text.getText().trim();
    if (content.length() > 0) {
      return content;
    }
    return null;
  }

}
