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

import com.google.cloud.tools.eclipse.appengine.localserver.runtime.CloudSdkRuntime;
import com.google.cloud.tools.eclipse.sdk.CloudSdkProvider;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import java.io.File;

/**
 * {@link WizardFragment} for configuring Google Cloud SDK Runtime.
 */
public final class CloudSdkRuntimeWizardFragment extends WizardFragment {
  private IWizardHandle wizard;
  private CloudSdkRuntime runtime;
  private Text dirTextBox;

  private Job validateLocationJob;

  @Override
  public Composite createComposite(Composite parent, IWizardHandle handle) {
    wizard = handle;
    runtime = getRuntimeDelegate();

    String title = getRuntimeTitle();
    wizard.setTitle("New " + title + " Runtime");
    wizard.setDescription("Define a new " + title + " runtime");

    // Do this before createContents() to prevent NPE; createContents() may set the dirTextBox,
    // triggering validation
    configureValidationJob();

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());
    createContents(composite);

    return composite;
  }

  private void configureValidationJob() {
    validateLocationJob = new Job("Validating Cloud SDK local server configuration") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        final IStatus runtimeStatus = runtime.validate();
        if (dirTextBox == null || dirTextBox.isDisposed()) {
          return Status.CANCEL_STATUS;
        }
        dirTextBox.getDisplay().asyncExec(new Runnable() {
          @Override
          public void run() {
            if (dirTextBox != null && !dirTextBox.isDisposed()) {
              if (runtimeStatus != null && !runtimeStatus.isOK()) {
                updateStatus(runtimeStatus.getMessage(), IStatus.ERROR);
              } else {
                updateStatus(null, IStatus.OK);
              }
              wizard.update();
            }
            validateLocationJob.done(Status.OK_STATUS);
          }
        });
        return ASYNC_FINISH;
      }
    };
    validateLocationJob.setSystem(true);
    validateLocationJob.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, true);
    validateLocationJob.setPriority(Job.SHORT);
  }

  @Override
  public boolean hasComposite() {
    return true;
  }

  private void createContents(final Composite composite) {
    Group group = new Group(composite, SWT.NONE);
    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    GridLayout layout = new GridLayout(6, true);
    group.setLayout(layout);
    group.setText("Google Cloud SDK");

    new Label(group, SWT.NONE).setText("SDK Directory:");

    dirTextBox = new Text(group, SWT.BORDER);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 4;
    dirTextBox.setLayoutData(data);
    dirTextBox.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        validate();
      }
    });

    Button button = new Button(group, SWT.PUSH);
    button.setText("&Browse...");
    button.addSelectionListener(new SelectionAdapter() {
      @Override
	  public void widgetSelected(SelectionEvent event) {
        DirectoryDialog dialog = new DirectoryDialog(composite.getShell(), SWT.OPEN);
        dialog.setText("Cloud SDK's Directory");
        dialog.setMessage("Select a directory");

        // It will return the selected directory, or
        // null if user cancels
        String dir = dialog.open();
        if (dir != null) {
          dirTextBox.setText(dir);
        }
      }
    });

    File location = new CloudSdkProvider(null).getCloudSdkLocation();
    if (location != null) {
      dirTextBox.setText(location.toString());
    }
  }

  private CloudSdkRuntime getRuntimeDelegate() {
    IRuntimeWorkingCopy workingCopy = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
    if (workingCopy == null) {
      return null;
    }
    return (CloudSdkRuntime) workingCopy.loadAdapter(CloudSdkRuntime.class, new NullProgressMonitor());
  }

  private String getRuntimeTitle() {
    IRuntimeType runtimeType = runtime.getRuntime().getRuntimeType();
    return runtimeType.getName();
  }

  private void updateStatus(String message, int newStatus) {
    switch (newStatus) {
      case IStatus.OK:
        setComplete(true);
        wizard.setMessage(null, IMessageProvider.NONE);
        break;
      case IStatus.INFO:
        setComplete(true);
        wizard.setMessage(message, IMessageProvider.INFORMATION);
        break;
    case IStatus.ERROR:
    default:
        setComplete(false);
      wizard.setMessage(message, IMessageProvider.ERROR);
      break;
    }
  }

  private void validate() {
    if (runtime == null) {
      updateStatus("Runtime delegate is missing or invalid", IStatus.ERROR);
      return;
    }

    updateStatus("Validating...", IStatus.INFO);
    Path path = new Path(dirTextBox.getText());
    runtime.getRuntimeWorkingCopy().setLocation(path);
    validateLocationJob.cancel();
    validateLocationJob.schedule(200); // small wait in case of more keystrokes
  }
}
