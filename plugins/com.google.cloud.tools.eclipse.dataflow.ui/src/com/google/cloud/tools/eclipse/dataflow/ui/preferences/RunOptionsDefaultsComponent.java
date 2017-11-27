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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
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
import com.google.cloud.tools.eclipse.dataflow.ui.util.SelectFirstMatchingPrefixListener;
import com.google.cloud.tools.eclipse.googleapis.GcpProjectServicesJob;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApi;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.projectselector.MiniSelector;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.ui.util.DisplayExecutor;
import com.google.cloud.tools.eclipse.ui.util.databinding.BucketNameValidator;
import com.google.cloud.tools.eclipse.util.jobs.Consumer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Collection;
import java.util.Objects;
import java.util.SortedSet;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
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

  /**
   * If true, then this component is allowed to be partially-complete.
   */
  private final boolean allowIncomplete;

  private final AccountSelector accountSelector;
  private final MiniSelector projectInput;
  private final Combo stagingLocationInput;
  private final Button createButton;

  private GcpProjectServicesJob checkProjectConfigurationJob;
  private SelectFirstMatchingPrefixListener completionListener;
  private ControlDecoration stagingLocationResults;

  @VisibleForTesting
  FetchStagingLocationsJob fetchStagingLocationsJob;
  @VisibleForTesting
  VerifyStagingLocationJob verifyStagingLocationJob;

  public RunOptionsDefaultsComponent(Composite target, int columns, MessageTarget messageTarget,
      DataflowPreferences preferences) {
    this(target, columns, messageTarget, preferences, null, false /* allowIncomplete */);
  }

  public RunOptionsDefaultsComponent(Composite target, int columns, MessageTarget messageTarget,
      DataflowPreferences preferences, WizardPage page, boolean allowIncomplete) {
    this(target, columns, messageTarget, preferences, page, allowIncomplete,
        PlatformUI.getWorkbench().getService(IGoogleLoginService.class),
        PlatformUI.getWorkbench().getService(IGoogleApiFactory.class));
  }

  @VisibleForTesting
  RunOptionsDefaultsComponent(Composite target, int columns, MessageTarget messageTarget,
      DataflowPreferences preferences, WizardPage page, boolean allowIncomplete,
      IGoogleLoginService loginService, IGoogleApiFactory apiFactory) {
    Preconditions.checkArgument(columns >= 3,
        "DefaultRunOptions must be in a Grid with at least 3 columns"); //$NON-NLS-1$
    this.target = target;
    this.page = page;
    this.messageTarget = messageTarget;
    displayExecutor = DisplayExecutor.create(target.getDisplay());
    this.apiFactory = apiFactory;
    this.allowIncomplete = allowIncomplete;

    Label accountLabel = new Label(target, SWT.NULL);
    accountLabel.setText(Messages.getString("account")); //$NON-NLS-1$
    accountSelector = new AccountSelector(target, loginService);

    Label projectInputLabel = new Label(target, SWT.NULL);
    projectInputLabel.setText(Messages.getString("cloud.platform.project.id")); //$NON-NLS-1$
    projectInput = new MiniSelector(target, apiFactory);

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
    projectInput.setCredential(accountSelector.getSelectedCredential());
    String projectId = preferences.getDefaultProject();
    projectInput.setProject(projectId);

    comboLabel.setText(Messages.getString("cloud.storage.staging.location")); //$NON-NLS-1$

    String stagingLocation = preferences.getDefaultStagingLocation();
    stagingLocationInput.setText(Strings.nullToEmpty(stagingLocation));

    // Account selection occupies a single row
    accountLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 1, 1));
    accountSelector.setLayoutData(
        new GridData(SWT.FILL, SWT.CENTER, true, false, columns - ACCOUNT_SPENT_COLUMNS, 1));

    // Project input occupies a single row
    projectInputLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 1, 1));
    projectInput.getControl().setLayoutData(
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
        projectInput.setCredential(accountSelector.getSelectedCredential());
        updateStagingLocations(0); // no delay
        validate();
      }
    });

    projectInput.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        updateStagingLocations(0); // no delay
        checkProjectConfiguration();
        validate();
      }
    });

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
    updateStagingLocations(0); // no delay
    messageTarget.setInfo(Messages.getString("set.pipeline.run.option.defaults")); //$NON-NLS-1$
    validate();
  }

  @VisibleForTesting
  void validate() {
    Preconditions.checkState(Display.getCurrent() != null, "Must be called on SWT UI thread");
    // may be from deferred event
    if (target.isDisposed()) {
      return;
    }

    // we set pageComplete to the value of `allowIncomplete` if the fields are valid
    setPageComplete(false);
    messageTarget.clear();

    Credential selectedCredential = accountSelector.getSelectedCredential();
    if (selectedCredential == null) {
      projectInput.setEnabled(false);
      stagingLocationInput.setEnabled(false);
      createButton.setEnabled(false);
      setPageComplete(allowIncomplete);
      return;
    }

    projectInput.setEnabled(true);
    if (projectInput.getProject() == null) {
      stagingLocationInput.setEnabled(false);
      createButton.setEnabled(false);
      setPageComplete(allowIncomplete);
      return;
    }

    if (checkProjectConfigurationJob != null && checkProjectConfigurationJob.isCurrent()) {
      Optional<Object> result = checkProjectConfigurationJob.getComputation();
      if (!result.isPresent()) {
        messageTarget.setInfo("Verifying that project is enabled for dataflow...");
        return;
      } else if (result.get() instanceof Exception) {
        DataflowUiPlugin.logError((Exception) result.get(),
            "Error checking project config for " + checkProjectConfigurationJob.getProjectId());
        if (result.get() instanceof GoogleJsonResponseException) {
          GoogleJsonResponseException exception = (GoogleJsonResponseException) result.get();
          messageTarget.setError("Error checking project: " + exception.getDetails().getMessage());
        } else {
          messageTarget.setError("Could not check project: " + result.get());
        }
        return;
      } else {
        Verify.verify(result.get() instanceof Collection);
        if (!((Collection<?>) result.get()).contains(GoogleApi.DATAFLOW_API.getServiceId())) {
          messageTarget.setError("Project is not enabled for Cloud Dataflow");
          return;
        }
      }
    }

    stagingLocationInput.setEnabled(true);

    // fetchStagingLocationsJob is a proxy for project checking
    if (fetchStagingLocationsJob != null) {
      if (fetchStagingLocationsJob.isCurrent()
          && fetchStagingLocationsJob.isComputationComplete()) {
        Optional<Exception> error = fetchStagingLocationsJob.getComputationError();
        if (error.isPresent()) {
          DataflowUiPlugin.logError(error.get(), "Exception while retrieving staging locations"); //$NON-NLS-1$
          messageTarget.setError(Messages.getString("could.not.retrieve.buckets.for.project", //$NON-NLS-1$
              projectInput.getProject().getName()));
          return;
        }
      } else {
        // check is still in progress or a new job is pending
        return;
      }
    }

    final String bucketNamePart = GcsDataflowProjectClient.toGcsBucketName(getStagingLocation());
    if (bucketNamePart.isEmpty()) {
      // If the bucket name is empty, we don't have anything to verify; and we don't have any
      // interesting messaging.
      createButton.setEnabled(false);
      setPageComplete(allowIncomplete);
      return;
    }

    IStatus status = bucketNameValidator.validate(bucketNamePart);
    if (!status.isOK()) {
      messageTarget.setError(status.getMessage());
      createButton.setEnabled(false);
      return;
    }

    Optional<Object> verificationResult =
        verifyStagingLocationJob != null && verifyStagingLocationJob.isCurrent()
            ? verifyStagingLocationJob.getComputation()
            : Optional.absent();
    if (!verificationResult.isPresent()) {
      messageTarget.setInfo("Verifying staging location...");
      createButton.setEnabled(false);
      return;
    } else if (verificationResult.get() instanceof Exception) {
      Exception error = (Exception) verificationResult.get();
      DataflowUiPlugin.logWarning("Unable to verify staging location", error);
      messageTarget.setError(Messages.getString("unable.verify.staging.location", bucketNamePart)); //$NON-NLS-1$
      return;
    } else {
      Verify.verify(verificationResult.get() instanceof VerifyStagingLocationResult);
      VerifyStagingLocationResult result = (VerifyStagingLocationResult) verificationResult.get();
      if (result.accessible) {
        messageTarget.setInfo(Messages.getString("verified.bucket.is.accessible", bucketNamePart)); //$NON-NLS-1$
        createButton.setEnabled(false);
        setPageComplete(true);
      } else {
        // user must create this bucket; feels odd that this is flagged as an error
        messageTarget.setError(Messages.getString("could.not.fetch.bucket", bucketNamePart)); //$NON-NLS-1$
        createButton.setEnabled(true);
      }
    }
  }

  protected void checkProjectConfiguration() {
    Credential selectedCredential = accountSelector.getSelectedCredential();
    if (selectedCredential == null) {
      return;
    }
    GcpProject project = projectInput.getProject();
    if (project == null) {
      return;
    }
    if (checkProjectConfigurationJob != null) {
      if (project.getId().equals(checkProjectConfigurationJob.getProjectId())) {
        // already in progress
        return;
      }
      checkProjectConfigurationJob.cancel();
    }

    checkProjectConfigurationJob =
        new GcpProjectServicesJob(apiFactory, selectedCredential, project.getId());
    checkProjectConfigurationJob.getFuture().addListener(new Runnable() {
      @Override
      public void run() {
        validate();
      }
    }, displayExecutor);
    checkProjectConfigurationJob.schedule();
  }

  private GcsDataflowProjectClient getGcsClient() {
    Preconditions.checkNotNull(accountSelector.getSelectedCredential());
    Credential credential = accountSelector.getSelectedCredential();
    return GcsDataflowProjectClient.create(apiFactory, credential);
  }

  public Control getControl() {
    return target;
  }

  /**
   * Return the selected account's email, or {@code ""} if no account selected.
   */
  public String getAccountEmail() {
    return accountSelector.getSelectedEmail();
  }

  public void selectAccount(String accountEmail) {
    accountSelector.selectAccount(accountEmail);
  }

  /**
   * Return the selected project, or {@code null} if no project selected.
   */
  public GcpProject getProject() {
    return projectInput.getProject();
  }

  /**
   * Return the selected project ID, or {@code ""} if no project selected.
   */
  public String getProjectId() {
    return projectInput.getProjectId();
  }

  public void setCloudProjectText(String project) {
    projectInput.setProject(project);
  }

  /**
   * Return the selected staging location, or {@code ""} if no staging location specified.
   */
  public String getStagingLocation() {
    return GcsDataflowProjectClient.toGcsLocationUri(stagingLocationInput.getText());
  }

  public void setStagingLocationText(String stagingLocation) {
    stagingLocationInput.setText(stagingLocation);
    // programmtically set so initiate check immediately
    startStagingLocationCheck(0);
  }

  public void addAccountSelectionListener(Runnable listener) {
    accountSelector.addSelectionListener(listener);
  }

  public void addModifyListener(final ModifyListener listener) {
    projectInput.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        Event dummy = new Event();
        dummy.widget = projectInput.getControl();
        listener.modifyText(new ModifyEvent(dummy));
      }
    });
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
  void updateStagingLocations(long scheduleDelay) {
    Credential credential = accountSelector.getSelectedCredential();
    String selectedEmail = accountSelector.getSelectedEmail();
    GcpProject project = projectInput.getProject();
    // Retrieving staging locations requires an authenticated user and project.
    // Check if there is an update is in progress; if it matches our user and project,
    // then quick-return, otherwise it is stale and should be cancelled.
    if (fetchStagingLocationsJob != null) {
      if (project != null
          && Objects.equals(project.getId(), fetchStagingLocationsJob.getProjectId())
          && Objects.equals(selectedEmail, fetchStagingLocationsJob.getAccountEmail())
          && fetchStagingLocationsJob.getState() == Job.RUNNING) {
        return;
      }
      fetchStagingLocationsJob.abandon();
    }
    fetchStagingLocationsJob = null;

    if (project != null && credential != null) {
      fetchStagingLocationsJob =
          new FetchStagingLocationsJob(getGcsClient(), selectedEmail, project.getId());
      fetchStagingLocationsJob.onSuccess(displayExecutor, new Consumer<SortedSet<String>>() {
        @Override
        public void accept(SortedSet<String> stagingLocations) {
          updateStagingLocations(stagingLocations);
          validate(); // reports message back to UI
        }});
      fetchStagingLocationsJob.onError(displayExecutor, new Consumer<Exception>() {
        @Override
        public void accept(Exception exception) {
          DataflowUiPlugin.logError(exception, "Exception while retrieving staging locations"); //$NON-NLS-1$
          validate();
        }});
      fetchStagingLocationsJob.schedule(scheduleDelay);
    }
  }

  /**
   * Update the suggested staging locations combo box with the provided locations.
   */
  protected void updateStagingLocations(SortedSet<String> stagingLocations) {
    if (target.isDisposed()) {
      return;
    }
    // Don't use "removeAll()", as it will clear the text field too.
    stagingLocationInput.remove(0, stagingLocationInput.getItemCount() - 1);
    for (String location : stagingLocations) {
      stagingLocationInput.add(location);
    }
    completionListener.setContents(stagingLocations);
    validate();
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
      verifyStagingLocationJob.abandon();
      verifyStagingLocationJob = null;
    }

    if (Strings.isNullOrEmpty(accountEmail) || Strings.isNullOrEmpty(stagingLocation)) {
      return;
    }

    verifyStagingLocationJob =
        new VerifyStagingLocationJob(getGcsClient(), accountEmail, stagingLocation);
    verifyStagingLocationJob.onSuccess(displayExecutor, new Runnable() {
      @Override
      public void run() {
        validate();
      }
    });
    verifyStagingLocationJob.schedule(schedulingDelay);
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
      // clear any error messages, such as bucket-does-not-exist
      messageTarget.clear();

      GcpProject project = getProject();
      String stagingLocation = getStagingLocation();
      StagingLocationVerificationResult result = getGcsClient()
          .createStagingLocation(project.getId(), stagingLocation, new NullProgressMonitor());
      if (result.isSuccessful()) {
        messageTarget.setInfo(Messages.getString("created.staging.location.at", stagingLocation)); //$NON-NLS-1$
        setPageComplete(true);
        createButton.setEnabled(false);
      } else {
        messageTarget
            .setError(Messages.getString("could.not.create.staging.location", stagingLocation)); //$NON-NLS-1$
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
