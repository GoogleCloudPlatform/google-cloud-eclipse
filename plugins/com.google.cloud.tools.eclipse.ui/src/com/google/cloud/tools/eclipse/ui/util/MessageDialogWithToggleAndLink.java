/*
 * Copyright 2022 Google LLC
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

package com.google.cloud.tools.eclipse.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.ErrorDialogErrorHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;


/**
 * Displays a dialog with a list of html links besides the message, button and state checkbox
 */
public class MessageDialogWithToggleAndLink extends MessageDialogWithToggle {

  private List<String> linkSources;
  private List<Link> links;

  /**
   * Creates a message dialog with a toggle. See the superclass constructor
   * for info on the other parameters.
   *
   * @param parentShell
   *            the parent shell
   * @param dialogTitle
   *            the dialog title, or <code>null</code> if none
   * @param image
   *            the dialog title image, or <code>null</code> if none
   * @param message
   *            the dialog message
   * @param dialogImageType
   *            one of the following values:
   *            <ul>
   *            <li><code>MessageDialog.NONE</code> for a dialog with no
   *            image</li>
   *            <li><code>MessageDialog.ERROR</code> for a dialog with an
   *            error image</li>
   *            <li><code>MessageDialog.INFORMATION</code> for a dialog
   *            with an information image</li>
   *            <li><code>MessageDialog.QUESTION </code> for a dialog with a
   *            question image</li>
   *            <li><code>MessageDialog.WARNING</code> for a dialog with a
   *            warning image</li>
   *            </ul>
   * @param defaultIndex
   *            the index in the button label array of the default button
   * @param toggleMessage
   *            the message for the toggle control, or <code>null</code> for
   *            the default message
   * @param toggleState
   *            the initial state for the toggle
   * @param linkSources
   *            HTML style list of links that will open a browser when clicked
   *
   */
  public MessageDialogWithToggleAndLink(Shell parentShell, String dialogTitle,
      Image image, String message, int dialogImageType,
      LinkedHashMap<String, Integer> buttonLabelToIdMap, int defaultIndex, String toggleMessage,
      boolean toggleState, List<String> linkSources) {
    super(parentShell, dialogTitle, image, message, dialogImageType, buttonLabelToIdMap, defaultIndex,
        toggleMessage, toggleState);
    this.linkSources = linkSources;
    this.links = new ArrayList<>();
  }

  /**
   * Create the area the message will be shown in.
   * <p>
   * The parent composite is assumed to use GridLayout as its layout manager,
   * since the parent is typically the composite created in
   * {@link Dialog#createDialogArea}.
   * </p>
   *
   * @param composite
   *            The composite to parent from.
   * @return Control
   */
  @Override
  protected Control createMessageArea( Composite parent ) {
    super.createMessageArea(parent);
    Composite container = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(1, true);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    container.setLayout(layout);
    container.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));
    linkSources.stream().forEach(link -> {
      Link newLink = new Link(container, SWT.WRAP);
      newLink.setText(link);
      newLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
      newLink.addSelectionListener(new OpenUriSelectionListener(new ErrorDialogErrorHandler(container.getShell())));
      links.add(newLink);
    });

    container.pack();
    return parent;
  }
  

  /**
   * Convenience method to open a simple confirm (OK/Cancel) dialog.
   *
   * @param parent
   *            the parent shell of the dialog, or <code>null</code> if none
   * @param title
   *            the dialog's title, or <code>null</code> if none
   * @param message
   *            the message
   * @param toggleMessage
   *            the message for the toggle control, or <code>null</code> for
   *            the default message
   * @param toggleState
   *            the initial state for the toggle
   * @return the dialog, after being closed by the user, which the client can
   *         only call <code>getReturnCode()</code> or
   *         <code>getToggleState()</code>
   * @param linkSources
   *            HTML style list of links that will open a browser when clicked
   */
  public static MessageDialogWithToggleAndLink openOkCancelConfirmLinks(
      Shell parent,
      String title, 
      String message, 
      String toggleMessage,
      boolean toggleState, 
      List<String> linkSources) {
    LinkedHashMap<String, Integer> buttons = new LinkedHashMap<>();
    buttons.put("Confirm", 0);
    MessageDialogWithToggleAndLink dialog = new MessageDialogWithToggleAndLink(parent, title, null, message, INFORMATION,
        buttons, 0, toggleMessage, toggleState, linkSources);
    int style = SWT.NONE;
    style &= SWT.SHEET;
    dialog.setShellStyle(dialog.getShellStyle() | style);
    dialog.setPrefStore(null);
    dialog.setPrefKey(null);
    dialog.open();
    return dialog;
  }

}
