package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;

import com.google.cloud.tools.eclipse.appengine.deploy.Messages;
import com.google.common.base.Preconditions;

/**
 * Prompts the user to select a directory for staging.
 */
// TODO should be moved to separate UI bundle (if kept at all in deploy's final implementation)
public class DialogStagingDirectoryProvider implements StagingDirectoryProvider {

  private Shell parentShell;
  private String stageDirPath;

  public DialogStagingDirectoryProvider(Shell shell) {
    Preconditions.checkNotNull(shell, "shell is null");
    this.parentShell = shell;
  }

  @Override
  public String get() {
    parentShell.getDisplay().syncExec(new Runnable() {

      @Override
      public void run() {
        DirectoryDialog directoryDialog = new DirectoryDialog(parentShell, SWT.APPLICATION_MODAL | SWT.SHEET);
        directoryDialog.setText(Messages.getString("dialog.staging.directory.provider.title")); //$NON-NLS-1$
        directoryDialog.setMessage(Messages.getString("dialog.staging.directory.provider.message")); //$NON-NLS-1$
        stageDirPath = directoryDialog.open();
      }
    });
    return stageDirPath;
  }
}
