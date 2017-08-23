/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.preferences;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.project.FetchStagingLocationsJob;
import com.google.cloud.tools.eclipse.dataflow.core.project.GcsDataflowProjectClient;
import com.google.cloud.tools.eclipse.dataflow.core.project.GcsDataflowProjectClient.StagingLocationVerificationResult;
import com.google.cloud.tools.eclipse.dataflow.core.project.VerifyStagingLocationJob;
import com.google.cloud.tools.eclipse.dataflow.core.project.VerifyStagingLocationJob.VerifyStagingLocationResult;
import com.google.cloud.tools.eclipse.dataflow.core.proxy.ListenableFutureProxy;
import com.google.cloud.tools.eclipse.dataflow.ui.DataflowUiPlugin;
import com.google.cloud.tools.eclipse.dataflow.ui.Messages;
import com.google.cloud.tools.eclipse.dataflow.ui.page.MessageTarget;
import com.google.cloud.tools.eclipse.dataflow.ui.util.ButtonFactory;
import com.google.cloud.tools.eclipse.dataflow.ui.util.DisplayExecutor;
import com.google.cloud.tools.eclipse.dataflow.ui.util.SelectFirstMatchingPrefixListener;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.ui.util.databinding.BucketNameValidator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSortedSet;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Collects default run options for Dataflow Pipelines and provides means to create and modify them.
 */
public class RunOptionsDefaultsComponent {

  private static final int PROJECT_INPUT_SPENT_COLUMNS = 1;
  private static final int STAGING_LOCATION_SPENT_COLUMNS = 2;
  private static final int ACCOUNT_SPENT_COLUMNS = 1;

  private static final long VERIFY_LOCATION_DELAY_MS = 250L;

  private final DisplayExecutor executor;
  private final MessageTarget messageTarget;
  private final Composite target;
  private final Text projectInput;
  private final Combo stagingLocationInput;
  private final Button createButton;
  private final AccountSelector accountSelector;
  private final IGoogleApiFactory apiFactory;

  private VerifyStagingLocationJob verifyJob;
  private SelectFirstMatchingPrefixListener completionListener;
  private WizardPage page = null;

  public RunOptionsDefaultsComponent(
      Composite target, int columns, MessageTarget messageTarget, DataflowPreferences preferences) {
    this(target, columns, messageTarget, preferences, null);
  }

  public RunOptionsDefaultsComponent(Composite target, int columns, MessageTarget messageTarget,
      DataflowPreferences preferences, WizardPage page) {
    this(target, columns, messageTarget, preferences, page,
        PlatformUI.getWorkbench().getService(IGoogleLoginService.class),
        PlatformUI.getWorkbench().getService(IGoogleApiFactory.class));
  }

  @VisibleForTesting
  RunOptionsDefaultsComponent(
      Composite target,
      int columns,
      MessageTarget messageTarget,
      DataflowPreferences preferences,
      WizardPage page,
      IGoogleLoginService loginService,
      IGoogleApiFactory apiFactory) {
    checkArgument(columns >= 3, "DefaultRunOptions must be in a Grid with at least 3 columns"); //$NON-NLS-1$
    this.target = target;
    this.page = page;
    this.messageTarget = messageTarget;
    this.executor = DisplayExecutor.create(target.getDisplay());
    this.apiFactory = apiFactory;

    Label accountLabel = new Label(target, SWT.NULL);
    accountLabel.setText(Messages.getString("account")); //$NON-NLS-1$
    accountSelector = new AccountSelector(target, loginService, 
        Messages.getString("sign.into.another.account")); //$NON-NLS-1$

    Label projectInputLabel = new Label(target, SWT.NULL);
    projectInput = new Text(target, SWT.SINGLE | SWT.BORDER);

    Label comboLabel = new Label(target, SWT.NULL);
    stagingLocationInput = new Combo(target, SWT.DROP_DOWN);
    createButton = ButtonFactory.newPushButton(target, 
        Messages.getString("create.bucket")); //$NON-NLS-1$
    createButton.setEnabled(false);

    accountSelector.selectAccount(preferences.getDefaultAccountEmail());

    projectInputLabel.setText(Messages.getString("cloud.platform.project.id")); //$NON-NLS-1$

    // Initialize the Default Project, which is used to populate the Staging Location field
    String project = preferences.getDefaultProject();
    projectInput.setText(Strings.nullToEmpty(project));

    comboLabel.setText(Messages.getString("cloud.storage.staging.location")); //$NON-NLS-1$

    String stagingLocation = preferences.getDefaultStagingLocation();
    stagingLocationInput.setText(Strings.nullToEmpty(stagingLocation));

    // Account selection occupies a single row
    accountLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 1, 1));
    accountSelector.setLayoutData(new GridData(
        SWT.FILL, SWT.CENTER, true, false, columns - ACCOUNT_SPENT_COLUMNS, 1));

    // Project input occupies a single row
    projectInputLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 1, 1));
    projectInput.setLayoutData(
        new GridData(SWT.FILL, SWT.CENTER, true, false, columns - PROJECT_INPUT_SPENT_COLUMNS, 1));

    // Staging Location, Combo, and Label occupy a single line
    comboLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 1, 1));
    stagingLocationInput.setLayoutData(new GridData(
        SWT.FILL, SWT.CENTER, true, false, columns - STAGING_LOCATION_SPENT_COLUMNS, 1));

    accountSelector.addSelectionListener(new Runnable() {
      @Override
      public void run() {
        // Don't use "removeAll()", as it will clear the text field too.
        stagingLocationInput.remove(0, stagingLocationInput.getItemCount() - 1);
        completionListener.setContents(ImmutableSortedSet.<String>of());
        updateStagingLocations(getProject());
      }
    });

    projectInput.addFocusListener(new GetProjectStagingLocationsListener());

    completionListener = new SelectFirstMatchingPrefixListener(stagingLocationInput);
    stagingLocationInput.addModifyListener(completionListener);
    stagingLocationInput.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        verifyStagingLocation(stagingLocationInput.getText());
      }
    });
    createButton.addSelectionListener(new CreateStagingLocationListener());

    stagingLocationInput.addModifyListener(new EnableCreateButton());

    updateStagingLocations(project);
    messageTarget.setInfo(Messages.getString("set.pipeline.run.option.defaults")); //$NON-NLS-1$
  }

  private GcsDataflowProjectClient getGcsClient() {
    Preconditions.checkNotNull(accountSelector.getSelectedCredential());
    Credential credential = accountSelector.getSelectedCredential();
    return GcsDataflowProjectClient.create(apiFactory, credential);
  }

  public Control getControl() {
    return target;
  }

  public void selectAccount(String accountEmail) {
    accountSelector.selectAccount(accountEmail);
  }

  public void setCloudProjectText(String project) {
    projectInput.setText(project);
  }

  public String getAccountEmail() {
    return accountSelector.getSelectedEmail();
  }

  public String getProject() {
    return projectInput.getText();
  }

  public void setStagingLocationText(String stagingLocation) {
    stagingLocationInput.setText(stagingLocation);
  }

  public String getStagingLocation() {
    return GcsDataflowProjectClient.toGcsLocationUri(stagingLocationInput.getText());
  }

  public void addAccountSelectionListener(Runnable listener) {
    accountSelector.addSelectionListener(listener);
  }

  public void addModifyListener(ModifyListener listener) {
    projectInput.addModifyListener(listener);
    stagingLocationInput.addModifyListener(listener);
  }

  public void setEnabled(boolean enabled) {
    accountSelector.setEnabled(enabled);
    projectInput.setEnabled(enabled);
    stagingLocationInput.setEnabled(enabled);
  }

  /**
   * Fetch the staging locations from GCS in a background task and update the Staging Locations
   * combo.
   */
  private void updateStagingLocations(String project) {
    // We can't retrieve staging locations if no project was input or we're not authenticated.
    Credential credential = accountSelector.getSelectedCredential();
    if (!Strings.isNullOrEmpty(project) && credential != null) {
      ListenableFutureProxy<SortedSet<String>> stagingLocationsFuture =
          FetchStagingLocationsJob.schedule(getGcsClient(), project);
      UpdateStagingLocationComboListener updateComboListener =
          new UpdateStagingLocationComboListener(stagingLocationsFuture);
      stagingLocationsFuture.addListener(updateComboListener, executor);
    }
  }

  /**
   * Ensure the staging location specified in the input combo is valid.
   */
  private void verifyStagingLocation(final String stagingLocation) {
    setPageComplete(false);
    messageTarget.clear();

    if (verifyJob != null) {
      // Cancel any existing verifyJob
      // FIXME: this has no effect, as "VerifyStagingLocationJob" doesn't honor cancellation.
      verifyJob.cancel();
    }

    final String bucketNamePart = extractBucketNamePart();
    if (bucketNamePart.isEmpty()) {
      // If the bucket name is empty, we don't have anything to verify; and we don't have any
      // interesting messaging.
      setPageComplete(true);
      return;
    }

    IStatus status = bucketNameStatus();
    if (!status.isOK()) {
      messageTarget.setError(status.getMessage());
      return;
    }

    if (accountSelector.getSelectedCredential() == null) {
      // We can't verify the staging location because no account was selected
      return;
    }

    String email = accountSelector.getSelectedEmail();
    verifyJob = VerifyStagingLocationJob.create(getGcsClient(), email, stagingLocation);
    verifyJob.schedule(VERIFY_LOCATION_DELAY_MS);
    final ListenableFutureProxy<VerifyStagingLocationResult> resultFuture =
        verifyJob.getVerifyResult();
    resultFuture.addListener(
        new Runnable() {
          @Override
          public void run() {
            if (target.isDisposed()) {
              return;
            }

            try {
              VerifyStagingLocationResult result = resultFuture.get();
              if (!result.email.equals(accountSelector.getSelectedEmail())
                  || !result.stagingLocation.equals(stagingLocationInput.getText())) {
                return;  // Input form changed; validation result no longer applicable
              }

              if (result.accessible) {
                messageTarget.setInfo(
                    Messages.getString("verified.bucket.is.accessible", bucketNamePart)); //$NON-NLS-1$
                createButton.setEnabled(false);
                setPageComplete(true);
              } else {
                messageTarget.setError(
                    Messages.getString("couldnt.fetch.bucket", bucketNamePart)); //$NON-NLS-1$
                createButton.setEnabled(true);
                setPageComplete(false);
              }
            } catch (InterruptedException | ExecutionException ex) {
              messageTarget.setError(
                  Messages.getString("couldnt.fetch.bucket", bucketNamePart)); //$NON-NLS-1$
              setPageComplete(false);
            }
          }
        },
        executor);
  }

  /**
   * Whenever focus is lost, retrieve all of the buckets and update the target combo with the
   * retrieved buckets, and update the {@link SelectFirstMatchingPrefixListener} with new
   * autocompletions.
   */
  private class GetProjectStagingLocationsListener extends FocusAdapter {
    @Override
    public void focusLost(FocusEvent event) {
      updateStagingLocations(getProject());
    }
  }

  /**
   * Create a GCS bucket in the project specified in the project input at the location specified in
   * the staging location input.
   */
  private class CreateStagingLocationListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent event) {
      if (accountSelector.getSelectedCredential() == null) {
        return;
      }

      String projectName = projectInput.getText();
      String stagingLocation = stagingLocationInput.getText();
      StagingLocationVerificationResult result = getGcsClient().createStagingLocation(
          projectName, stagingLocation, new NullProgressMonitor());
      if (result.isSuccessful()) {
        messageTarget.setInfo(
            Messages.getString("created.staging.location.at", stagingLocation)); //$NON-NLS-1$
        setPageComplete(true);
        createButton.setEnabled(false);
      } else {
        messageTarget.setError(
            Messages.getString("could.not.create.staging.location", stagingLocation)); //$NON-NLS-1$
        setPageComplete(false);
      }
    }
  }

  private void setPageComplete(boolean complete) {
    if (page != null) {
      page.setPageComplete(complete);
    }
  }

  /**
   * A job change listener that updates a Combo with a collection of retrieved staging locations
   * when the listened job completes.
   */
  private class UpdateStagingLocationComboListener implements Runnable {
    private final Future<SortedSet<String>> stagingLocationsFuture;

    UpdateStagingLocationComboListener(Future<SortedSet<String>> stagingLocationsFuture) {
      this.stagingLocationsFuture = stagingLocationsFuture;
    }

    /**
     * Update the Combo with the staging locations retrieved by the Job.
     */
    @Override
    public void run() {
      if (target.isDisposed()) {
        return;
      }

      try {
        SortedSet<String> stagingLocations = stagingLocationsFuture.get();
        messageTarget.clear();
        // Don't use "removeAll()", as it will clear the text field too.
        stagingLocationInput.remove(0, stagingLocationInput.getItemCount() - 1);
        for (String location : stagingLocations) {
          stagingLocationInput.add(location);
        }
        completionListener.setContents(stagingLocations);
      } catch (InterruptedException | ExecutionException ex) {
        messageTarget.setError(
            Messages.getString("could.not.retrieve.buckets.for.project", projectInput.getText())); //$NON-NLS-1$
        DataflowUiPlugin.logError(ex, "Exception while retrieving staging locations"); //$NON-NLS-1$
        setPageComplete(false);
      }
    }
  }

  private static final BucketNameValidator bucketNameValidator = new BucketNameValidator();

  private String extractBucketNamePart() {
    // Don't trim text unless you consistently trim everywhere else (e.g., "getStagingLocation()").
    return GcsDataflowProjectClient.toGcsBucketName(stagingLocationInput.getText());
  }

  @VisibleForTesting
  IStatus bucketNameStatus() {
    return bucketNameValidator.validate(extractBucketNamePart());
  }

  private class EnableCreateButton implements ModifyListener {

    @Override
    public void modifyText(ModifyEvent event) {
      boolean enabled = !extractBucketNamePart().isEmpty() && bucketNameStatus().isOK();
      createButton.setEnabled(enabled);
    }

  }

}
