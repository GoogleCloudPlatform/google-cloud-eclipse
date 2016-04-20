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

import java.util.List;

import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.google.cloud.tools.eclipse.appengine.localserver.server.CloudSdkServerFlags;
import com.google.cloud.tools.eclipse.appengine.localserver.server.ServerFlagsInfo.Flag;

/**
 * A dialog that prompts the user to choose and configure a "gcloud app run"
 * flag.
 */
@SuppressWarnings("restriction") // For SWTFactory
public class ServerFlagSelectionDialog extends ElementListSelectionDialog {
  private static final String FLAG_PREFIX = "--";
  private Text descriptionText;
  private Text argumentText;
  private String argumentValue;

  /**
   * Constructs a new flag selection dialog.
   *
   * @param parent parent shell
   */
  public ServerFlagSelectionDialog(Shell parent) {
    super(parent, new ServerFlagLabelProvider());
    setShellStyle(getShellStyle() | SWT.RESIZE);
    setTitle("Select Variable");
    setMessage("&Choose a variable (? = any character, * = any string):");
    setMultipleSelection(false);

    List<Flag> flags = CloudSdkServerFlags.getFlags();
    setElements(flags.toArray(new Flag[flags.size()]));
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Control control = super.createDialogArea(parent);
    createArgumentArea((Composite) control);
    return control;
  }

  /**
   * Update variable description and argument button enablement.
   */
  @Override
  protected void handleSelectionChanged() {
    super.handleSelectionChanged();
    Object[] objects = getSelectedElements();
    boolean argEnabled = false;
    String text = "";
    if (objects.length == 1) {
      Flag variable = (Flag) objects[0];
      argEnabled = variable.getSupportsArgument();
      text = variable.getDescription();
    }
    argumentText.setEnabled(argEnabled);
    descriptionText.setText(text);
  }

  @Override
  protected void okPressed() {
    argumentValue = argumentText.getText().trim();
    super.okPressed();
  }

  /**
   * Returns the variable expression the user generated from this dialog, or
   * {@code null} if none.
   *
   * @return variable expression the user generated from this dialog, or
   *         {@code null} if none
   */
  public String getVariableExpression() {
    Object[] selected = getResult();
    if (selected != null && selected.length == 1) {
      Flag variable = (Flag) selected[0];
      StringBuilder variableExpressionBuilder = new StringBuilder();
      variableExpressionBuilder.append(FLAG_PREFIX);
      variableExpressionBuilder.append(variable.getName());
      if (argumentValue != null && argumentValue.length() > 0) {
        variableExpressionBuilder.append(" ");
        variableExpressionBuilder.append(argumentValue);
      }
      variableExpressionBuilder.append(" ");
      return variableExpressionBuilder.toString();
    }
    return null;
  }

  /**
   * Creates an area to display a description of the selected variable and a
   * text box to accept the variable's argument.
   *
   * @param parent parent widget
   */
  private void createArgumentArea(Composite parent) {
    Composite container = SWTFactory.createComposite(parent,
                                                     parent.getFont(),
                                                     /* columns */ 2,
                                                     /* hspan */ 1,
                                                     GridData.FILL_HORIZONTAL,
                                                     /* marginWidht */ 0,
                                                     /* marginHeight */ 0);
    SWTFactory.createWrapLabel(container, "&Argument:", /* hspan */ 2);

    argumentText = new Text(container, SWT.BORDER);
    argumentText.setFont(container.getFont());
    argumentText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    SWTFactory.createWrapLabel(container, "&Variable Description:", /* hspan */ 2);

    descriptionText = new Text(container, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
    descriptionText.setFont(container.getFont());
    descriptionText.setEditable(false);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    gridData.heightHint = 50;
    descriptionText.setLayoutData(gridData);
  }
}
