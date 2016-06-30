package com.google.cloud.tools.eclipse.usagetracker;

import org.eclipse.swt.widgets.Shell;


/**
 * The only purpose of the class is to allow unit testing for
 * {@link AnalyticsPingManager#showOptInDialog}.
 */
public class OptInDialogCreator {

  public OptInDialog create(Shell parentShell) {
    return new OptInDialog(parentShell);
  }
}
