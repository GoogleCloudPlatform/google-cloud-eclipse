
package com.google.cloud.tools.eclipse.test.util.ui;

import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ThreadDumpHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ThreadDumpingWatchdog.report();
    return null;
  }
}
