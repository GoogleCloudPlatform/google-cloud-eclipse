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
import java.util.Objects;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
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
 * Assumed to be executed solely within the SWT UI thread.
 */
public class RunOptionsDefaultsComponent {
  private static final int PROJECT_INPUT_SPENT_COLUMNS = 1;
  private static final int STAGING_LOCATION_SPENT_COLUMNS = 2;
  private static final int ACCOUNT_SPENT_COLUMNS = 1;

  /** Milliseconds to wait after a key before launching a job, to avoid needless computation. */
  private static final long NEXT_KEY_DELAY_MS = 250L;

  private static final BucketNameValidator bucketNameValidator = new BucketNameValidator();

  private final IGoogleApiFactory apiFactory;
  private final WizardPage page;
  private final DisplayExecutor displayExecutor;
  private final MessageTarget messageTarget;
  private final Composite target;
  
  private final AccountSelector accountSelector;
  private final Text projectInput;
  private final Combo stagingLocationInput;
  private final Button createButton;
  private SelectFirstMatchingPrefixListener completionListener;
  private ControlDecoration stagingLocationResults;

  private FetchStagingLocationsJob fetchStagingLocationsJob;
  @VisibleForTesting
  VerifyStagingLocationJob verifyStagingLocationJob;

  public RunOptionsDefaultsComponent(Composite target, int columns, MessageTarget messageTarget,
      DataflowPreferences preferences) {
    this(target, columns, messageTarget, preferences, null);
  }

  public RunOptionsDefaultsComponent(Composite target, int columns, MessageTarget messageTarget,
      DataflowPreferences preferences, WizardPage page) {
    this(target, columns, messageTarget, preferences, page,
        PlatformUI.getWorkbench().getService(IGoogleLoginService.class),
        PlatformUI.getWorkbench().getService(IGoogleApiFactory.class));
  }

  @VisibleForTesting
  RunOptionsDefaultsComponent(Composite target, int columns, MessageTarget messageTarget,
      DataflowPreferences preferences, WizardPage page, IGoogleLoginService loginService,
      IGoogleApiFactory apiFactory) {
    checkArgument(columns >= 3, "DefaultRunOptions must be in a Grid with at least 3 columns"); //$NON-NLS-1$
    this.target = target;
    this.page = page;
    this.messageTarget = messageTarget;
    this.displayExecutor = DisplayExecutor.create(target.getDisplay());
    this.apiFactory = apiFactory;

    Label accountLabel = new Label(target, SWT.NULL);
    accountLabel.setText(Messages.getString("account")); //$NON-NLS-1$
    accountSelector =
        new AccountSelector(target, loginService, Messages.getString("sign.into.another.account")); //$NON-NLS-1$

    Label projectInputLabel = new Label(target, SWT.NULL);
    projectInputLabel.setText(Messages.getString("cloud.platform.project.id")); //$NON-NLS-1$
    projectInput = new Text(target, SWT.SINGLE | SWT.BORDER);

    Label comboLabel = new Label(target, SWT.NULL);
    stagingLocationInput = new Combo(target, SWT.DROP_DOWN);
    createButton = ButtonFactory.newPushButton(target, Messages.getString("create.bucket")); //$NON-NLS-1$
    createButton.setEnabled(false);

    FieldDecorationRegistry registry = FieldDecorationRegistry.getDefault();
    stagingLocationResults = new ControlDecoration(stagingLocationInput, SWT.TOP | SWT.LEFT);
    // error image is required for the hover to be correctly placed
    Image errorImage = registry.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage();
    stagingLocationResults.setImage(errorImage);
    stagingLocationResults.hide();

    accountSelector.selectAccount(preferences.getDefaultAccountEmail());

    // Initialize the Default Project, which is used to populate the Staging Location field
    String project = preferences.getDefaultProject();
    projectInput.setText(Strings.nullToEmpty(project));

    comboLabel.setText(Messages.getString("cloud.storage.staging.location")); //$NON-NLS-1$

    String stagingLocation = preferences.getDefaultStagingLocation();
    stagingLocationInput.setText(Strings.nullToEmpty(stagingLocation));

    // Account selection occupies a single row
    accountLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 1, 1));
    accountSelector.setLayoutData(
        new GridData(SWT.FILL, SWT.CENTER, true, false, columns - ACCOUNT_SPENT_COLUMNS, 1));

    // Project input occupies a single row
    projectInputLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 1, 1));
    projectInput.setLayoutData(
        new GridData(SWT.FILL, SWT.CENTER, true, false, columns - PROJECT_INPUT_SPENT_COLUMNS, 1));

    // Staging Location, Combo, and Label occupy a single line
    comboLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 1, 1));
    stagingLocationInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
        columns - STAGING_LOCATION_SPENT_COLUMNS, 1));

    accountSelector.addSelectionListener(new Runnable() {
      @Override
      public void run() {
        // Don't use "removeAll()", as it will clear the text field too.
        stagingLocationInput.remove(0, stagingLocationInput.getItemCount() - 1);
        completionListener.setContents(ImmutableSortedSet.<String>of());
        updateStagingLocations(getProject(), 0); // no delay
        validate();
      }
    });

    projectInput.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateStagingLocations(getProject(), NEXT_KEY_DELAY_MS);
        validate();
      }
    });
    projectInput.addFocusListener(new GetProjectStagingLocationsListener());

    completionListener = new SelectFirstMatchingPrefixListener(stagingLocationInput);
    stagingLocationInput.addModifyListener(completionListener);
    stagingLocationInput.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        startStagingLocationCheck(NEXT_KEY_DELAY_MS);
        stagingLocationResults.hide();
        validate();
      }
    });
    createButton.addSelectionListener(new CreateStagingLocationListener());

    startStagingLocationCheck(0); // no delay
    updateStagingLocations(project, 0); // no delay
    messageTarget.setInfo(Messages.getString("set.pipeline.run.option.defaults")); //$NON-NLS-1$
    validate();
  }

  private void validate() {
    setPageComplete(false);
    Credential selectedCredential = accountSelector.getSelectedCredential();
    if (selectedCredential == null) {
      projectInput.setEnabled(false);
      stagingLocationInput.setEnabled(false);
      createButton.setEnabled(false);
      return;
    }

    projectInput.setEnabled(true);
    if (Strings.isNullOrEmpty(projectInput.getText())) {
      stagingLocationInput.setEnabled(false);
      createButton.setEnabled(false);
      return;
    }
    // FIXME: incorporate project verification here

    stagingLocationInput.setEnabled(true);

    final String bucketNamePart = GcsDataflowProjectClient.toGcsBucketName(getStagingLocation());
    if (bucketNamePart.isEmpty()) {
      // If the bucket name is empty, we don't have anything to verify; and we don't have any
      // interesting messaging.
      createButton.setEnabled(false);
      return;
    }

    IStatus status = bucketNameValidator.validate(bucketNamePart);
    if (!status.isOK()) {
      messageTarget.setError(status.getMessage());
      createButton.setEnabled(false);
      return;
    }
    
    if (fetchStagingLocationsJob != null) {
      Future<SortedSet<String>> stagingLocationsFuture =
          fetchStagingLocationsJob.getStagingLocations();
      if (stagingLocationsFuture.isDone()) {
        try {
          // on error, will raise an exception
          stagingLocationsFuture.get();
        } catch (ExecutionException ex) {
          messageTarget.setError(Messages.getString("could.not.retrieve.buckets.for.project", //$NON-NLS-1$
              projectInput.getText()));
          DataflowUiPlugin.logError(ex, "Exception while retrieving staging locations"); //$NON-NLS-1$
          return;
        } catch (InterruptedException ex) {
          DataflowUiPlugin.logError(ex, "Interrupted while retrieving staging locations"); //$NON-NLS-1$
        }
      }
    }

    if (verifyStagingLocationJob == null) {
      messageTarget.setInfo("Verifying staging location...");
      createButton.setEnabled(false);
      return;
    }
    Future<VerifyStagingLocationResult> verifyStagingLocationFuture = verifyStagingLocationJob.getVerifyResult();
    if (!verifyStagingLocationFuture.isDone()) {
      messageTarget.setInfo("Verifying staging location...");
      createButton.setEnabled(false);
      return;
    }

    try {
      VerifyStagingLocationResult result = verifyStagingLocationFuture.get();
      if (!result.email.equals(accountSelector.getSelectedEmail())
          || !result.stagingLocation.equals(getStagingLocation())) {
        // stale; perhaps we should initiate verification of the staging location?
        createButton.setEnabled(false);
        return;
      }

      if (result.accessible) {
        messageTarget.setInfo(Messages.getString("verified.bucket.is.accessible", bucketNamePart)); //$NON-NLS-1$
        createButton.setEnabled(false);
        setPageComplete(true);
      } else {
        // user must create this bucket; feels odd that this is flagged as an error
        messageTarget.setError(Messages.getString("could.not.fetch.bucket", bucketNamePart)); //$NON-NLS-1$
        createButton.setEnabled(true);
      }
    } catch (InterruptedException | ExecutionException ex) {
      DataflowUiPlugin.logWarning("Unable to verify staging location", ex);
      messageTarget.setError(Messages.getString("unable.verify.staging.location", bucketNamePart)); //$NON-NLS-1$
    }
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
  @VisibleForTesting
  void updateStagingLocations(String project, long scheduleDelay) {
    Credential credential = accountSelector.getSelectedCredential();
    String selectedEmail = accountSelector.getSelectedEmail();
    // Retrieving staging locations requires an authenticated user and project.
    // Check if there is an update is in progress; if it matches our user and project,
    // then quick-return, otherwise it is stale and should be cancelled.
    if (fetchStagingLocationsJob != null) {
      if (Objects.equals(project, fetchStagingLocationsJob.getProject())
          && Objects.equals(selectedEmail, fetchStagingLocationsJob.getAccountEmail())
          && fetchStagingLocationsJob.getState() == Job.RUNNING) {
        return;
      }
      fetchStagingLocationsJob.cancel();
    }
    fetchStagingLocationsJob = null;

    if (!Strings.isNullOrEmpty(project) && credential != null) {
      final FetchStagingLocationsJob thisJob = fetchStagingLocationsJob =
          new FetchStagingLocationsJob(getGcsClient(), selectedEmail, project);
      fetchStagingLocationsJob.getStagingLocations().addListener(new Runnable() {
        @Override
        public void run() {
          // check that this is same job (may have been cancelled in the interim)
          if (!target.isDisposed() && fetchStagingLocationsJob == thisJob) {
            // Update the Combo with the staging locations retrieved by the Job.
            try {
              SortedSet<String> stagingLocations = fetchStagingLocationsJob.getStagingLocations().get();
              // Don't use "removeAll()", as it will clear the text field too.
              stagingLocationInput.remove(0, stagingLocationInput.getItemCount() - 1);
              for (String location : stagingLocations) {
                stagingLocationInput.add(location);
              }
              completionListener.setContents(stagingLocations);
            } catch (InterruptedException | ExecutionException ex) {
              // ignored: handled by validate()
            }
            validate();
          }
        }
      }, displayExecutor);
      fetchStagingLocationsJob.schedule(scheduleDelay);
    }
  }

  /**
   * Ensure the staging location specified in the input combo is valid.
   */
  @VisibleForTesting
  void startStagingLocationCheck(long schedulingDelay) {
    String accountEmail = getAccountEmail();
    String stagingLocation = getStagingLocation();

    if (verifyStagingLocationJob != null) {
      if (Objects.equals(accountEmail, verifyStagingLocationJob.getEmail())
          && Objects.equals(stagingLocation, verifyStagingLocationJob.getStagingLocation())
          && verifyStagingLocationJob.getState() == Job.RUNNING) {
        // an update is in progress
        return;
      }
      // Cancel any existing verifyStagingLocationJob
      verifyStagingLocationJob.cancel();
      verifyStagingLocationJob = null;
    }

    if (Strings.isNullOrEmpty(accountEmail) || Strings.isNullOrEmpty(stagingLocation)) {
      return;
    }

    final VerifyStagingLocationJob thisJob = this.verifyStagingLocationJob =
        new VerifyStagingLocationJob(getGcsClient(), accountEmail, stagingLocation);
    verifyStagingLocationJob.getVerifyResult().addListener(new Runnable() {
      @Override
      public void run() {
        // check that this is same job (may have been cancelled in the interim)
        if (!target.isDisposed() && verifyStagingLocationJob == thisJob) {
          validate();
        }
      }
    }, displayExecutor);
    verifyStagingLocationJob.schedule(schedulingDelay);
  }

  /**
   * Whenever focus is lost, retrieve all of the buckets and update the target combo with the
   * retrieved buckets, and update the {@link SelectFirstMatchingPrefixListener} with new
   * autocompletions.
   */
  private class GetProjectStagingLocationsListener extends FocusAdapter {
    @Override
    public void focusLost(FocusEvent event) {
      updateStagingLocations(getProject(), 0); // no delay
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
      stagingLocationResults.hide();

      String projectName = getProject();
      String stagingLocation = getStagingLocation();
      StagingLocationVerificationResult result = getGcsClient().createStagingLocation(projectName,
          stagingLocation, new NullProgressMonitor());
      if (result.isSuccessful()) {
        messageTarget.setInfo(Messages.getString("created.staging.location.at", stagingLocation)); //$NON-NLS-1$
        setPageComplete(true);
        createButton.setEnabled(false);
      } else {
        messageTarget.setError(
            Messages.getString("could.not.create.staging.location", stagingLocation)); //$NON-NLS-1$
        stagingLocationResults.show();
        stagingLocationResults.showHoverText(result.getMessage());
        setPageComplete(false);
      }
    }
  }

  private void setPageComplete(boolean complete) {
    if (page != null) {
      page.setPageComplete(complete);
    }
  }
}
