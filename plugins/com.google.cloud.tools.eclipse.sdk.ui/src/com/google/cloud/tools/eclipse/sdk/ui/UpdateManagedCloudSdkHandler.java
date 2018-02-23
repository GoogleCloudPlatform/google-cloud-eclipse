
package com.google.cloud.tools.eclipse.sdk.ui;

import com.google.cloud.tools.eclipse.sdk.CloudSdkManager;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/** Trigger an update of the managed Google Cloud SDK. */
public class UpdateManagedCloudSdkHandler extends AbstractHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    CloudSdkManager.getInstance().updateManagedSdkAsync();
    return null;
  }
}
