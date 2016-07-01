package com.google.cloud.tools.eclipse.usagetracker;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * A one-time dialog to suggest opt-in for sending client-side usage metrics.
 */
public class OptInDialog extends Dialog {

  public OptInDialog(Shell parentShell) {
    super(parentShell);
    setShellStyle(SWT.TITLE | SWT.CLOSE | SWT.MODELESS);
    setBlockOnOpen(false);
  }

  /**
   * Show this dialog at the top-right corner of some target window.
   */
  @Override
  protected Point getInitialLocation(Point initialSize) {
    Shell targetShell = findTargetShell();
    if (targetShell == null) {
      return super.getInitialLocation(initialSize);
    }

    // Position the dialog at the top-right corner of the targetShell window.
    Rectangle parentBounds = targetShell.getBounds();
    Rectangle parentClientArea = targetShell.getClientArea();

    int heightCaptionAndUpperBorder =
        parentBounds.height - parentClientArea.height - targetShell.getBorderWidth();
    return new Point(parentBounds.x + parentClientArea.width - initialSize.x,
        parentBounds.y + heightCaptionAndUpperBorder);
  }

  /**
   * Strongly prefer returning the shell of the currently active workbench as a target
   * window to position the dialog at the top-right corner. If we can't get a workbench
   * for any reason, fall back to the parent shell (which can be null).
   */
  private Shell findTargetShell() {
    try {
      IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      return window != null ? window.getShell() : getParentShell();
    } catch (IllegalStateException ise) {  // getWorkbench() might throw this.
      return getParentShell();
    }
  }

  /**
   * Set the dialog title.
   */
  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(Messages.OPT_IN_DIALOG_TITLE);
  }

  /**
   * Create buttons.
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, Messages.OPT_IN_BUTTON, false);
    createButton(parent, IDialogConstants.CANCEL_ID, Messages.OPT_OUT_BUTTON, true);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);

    Label label = new Label(container, SWT.WRAP);
    label.setText(Messages.OPT_IN_DIALOG_TEXT);

    return container;
  }

  @Override
  protected void okPressed() {
    super.okPressed();
    AnalyticsPingManager.getInstance().registerOptInStatus(true);
  }

  @Override
  protected void cancelPressed() {
    super.cancelPressed();
    AnalyticsPingManager.getInstance().registerOptInStatus(false);
  }

  /**
   * When the dialog closes in other ways than pressing the buttons.
   */
  @Override
  protected void handleShellCloseEvent() {
    super.handleShellCloseEvent();
    AnalyticsPingManager.getInstance().registerOptInStatus(false);
  }
}
