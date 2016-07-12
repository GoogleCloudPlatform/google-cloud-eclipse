package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;

import com.google.cloud.tools.eclipse.appengine.deploy.Messages;

/**
 * Prompts the user to select a directory for staging.
 */
// TODO should be moved to separate UI bundle (if kept at all in deploy's final implementation)
public class DialogStagingDirectoryProvider implements StagingDirectoryProvider {

  private Shell parentShell;

  public DialogStagingDirectoryProvider(Shell shell) {
    this.parentShell = shell;
  }

  @Override
  public String get() {
    DirectoryDialog directoryDialog = new DirectoryDialog(parentShell, SWT.APPLICATION_MODAL | SWT.SHEET);
    directoryDialog.setText(Messages.getString("dialog.staging.directory.provider.title")); //$NON-NLS-1$
    directoryDialog.setMessage(Messages.getString("dialog.staging.directory.provider.message")); //$NON-NLS-1$
    return directoryDialog.open();
  }
}
