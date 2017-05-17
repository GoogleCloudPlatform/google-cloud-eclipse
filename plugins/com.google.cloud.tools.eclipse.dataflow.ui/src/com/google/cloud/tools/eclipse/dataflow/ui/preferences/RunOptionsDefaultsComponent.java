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

import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.project.FetchStagingLocationsJob;
import com.google.cloud.tools.eclipse.dataflow.core.project.GcsDataflowProjectClient;
import com.google.cloud.tools.eclipse.dataflow.core.project.VerifyStagingLocationJob;
import com.google.cloud.tools.eclipse.dataflow.core.project.VerifyStagingLocationJob.VerifyStagingLocationResult;
import com.google.cloud.tools.eclipse.dataflow.core.proxy.ListenableFutureProxy;
import com.google.cloud.tools.eclipse.dataflow.core.util.CouldNotCreateCredentialsException;
import com.google.cloud.tools.eclipse.dataflow.ui.DataflowUiPlugin;
import com.google.cloud.tools.eclipse.dataflow.ui.page.MessageTarget;
import com.google.cloud.tools.eclipse.dataflow.ui.util.ButtonFactory;
import com.google.cloud.tools.eclipse.dataflow.ui.util.DisplayExecutor;
import com.google.cloud.tools.eclipse.dataflow.ui.util.SelectFirstMatchingPrefixListener;
import com.google.cloud.tools.eclipse.dataflow.ui.util.SelectFirstMatchingPrefixListener.OnCompleteListener;
import com.google.common.base.Strings;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.statushandlers.StatusManager;

import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Collects default run options for Dataflow Pipelines and provides means to create and modify them.
 */
public class RunOptionsDefaultsComponent {

  private static final int PROJECT_INPUT_SPENT_COLUMNS = 1;
  private static final int STAGING_LOCATION_SPENT_COLUMNS = 2;

  private static final long VERIFY_LOCATION_DELAY_MS = 250L;

  private final GcsDataflowProjectClient client;
  private final DisplayExecutor executor;

  private final MessageTarget messageTarget;

  private final Composite target;
  private final Text projectInput;
  private final Combo stagingLocationInput;
  private final Button createButton;

  private VerifyStagingLocationJob verifyJob;
  private SelectFirstMatchingPrefixListener completionListener;

  public RunOptionsDefaultsComponent(
      Composite target, int columns, MessageTarget messageTarget, DataflowPreferences preferences) {
    checkArgument(columns >= 3, "DefaultRunOptions must be in a Grid with at least 3 columns");
    this.target = target;
    this.messageTarget = messageTarget;
    this.executor = DisplayExecutor.create(target.getDisplay());
    this.client = setupGcsClient();

    Label projectInputLabel = new Label(target, SWT.NULL);
    projectInput = new Text(target, SWT.SINGLE | SWT.BORDER);

    Label comboLabel = new Label(target, SWT.NULL);
    stagingLocationInput = new Combo(target, SWT.DROP_DOWN);
    createButton = ButtonFactory.newPushButton(target, "&Create");
    createButton.setEnabled(false);

    // Initialize the Default Project, which is used to populate the Staging Location field
    String project = preferences.getDefaultProject();
    if (project == null) {
      project = "";
    }

    projectInputLabel.setText("Cloud Platform &Project ID:");
    projectInput.setText(project);

    comboLabel.setText("Cloud Storage Staging &Location:");

    String stagingLocation = preferences.getDefaultStagingLocation();
    if (stagingLocation == null) {
      stagingLocation = "";
    }
    stagingLocationInput.setText(stagingLocation);

    // Project input occupies a single row
    projectInputLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 1, 1));
    projectInput.setLayoutData(
        new GridData(SWT.FILL, SWT.CENTER, true, false, columns - PROJECT_INPUT_SPENT_COLUMNS, 1));

    // Staging Location, Combo, and Label occupy a single line
    comboLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 1, 1));
    stagingLocationInput.setLayoutData(new GridData(
        SWT.FILL, SWT.CENTER, true, false, columns - STAGING_LOCATION_SPENT_COLUMNS, 1));

    GetProjectStagingLocationsListener getProjectStagingLocationsListener =
        new GetProjectStagingLocationsListener();
    projectInput.addFocusListener(getProjectStagingLocationsListener);

    completionListener = new SelectFirstMatchingPrefixListener(stagingLocationInput);
    stagingLocationInput.addModifyListener(completionListener);
    completionListener.addOnCompleteListener(new OnCompleteListener() {
      @Override
      public void onComplete(String contents) {
        verifyStagingLocation(contents);
      }
    });
    createButton.addSelectionListener(new CreateStagingLocationListener());

    stagingLocationInput.addModifyListener(new EnableCreateButton());
    
    updateStagingLocations(project);
    messageTarget.setInfo("Set Pipeline Run Option Defaults");
  }

  private static GcsDataflowProjectClient setupGcsClient() {
    try {
      return GcsDataflowProjectClient.createWithDefaultClient();
    } catch (CouldNotCreateCredentialsException e) {
      StatusManager.getManager().handle(e.getStatus(), StatusManager.SHOW);
      return null;
    }
  }

  public Control getControl() {
    return this.target;
  }

  public void setCloudProjectText(String project) {
    projectInput.setText(project);
  }

  public String getProject() {
    return projectInput.getText();
  }

  public void setStagingLocationText(String stagingLocation) {
    stagingLocationInput.setText(stagingLocation);
  }

  public String getStagingLocation() {
    return client.toGcsLocationUri(stagingLocationInput.getText());
  }

  public void addProjectModifyListener(ModifyListener listener) {
    projectInput.addModifyListener(listener);
  }

  public void addStagingLocationModifyListener(ModifyListener listener) {
    stagingLocationInput.addModifyListener(listener);
  }

  public void addModifyListener(ModifyListener listener) {
    addProjectModifyListener(listener);
    addStagingLocationModifyListener(listener);
  }

  public void setEnabled(boolean enabled) {
    projectInput.setEnabled(enabled);
    stagingLocationInput.setEnabled(enabled);
    createButton.setEnabled(enabled);
  }

  /**
   * Fetch the staging locations from GCS in a background task and update the Staging Locations
   * combo.
   */
  private void updateStagingLocations(String project) {
    // We can't retrieve staging locations because no project was input or we're not authenticated.
    if (!Strings.isNullOrEmpty(project) && client != null) {
      ListenableFutureProxy<SortedSet<String>> stagingLocationsFuture =
          FetchStagingLocationsJob.schedule(client, project);
      UpdateStagingLocationComboListener updateComboListener =
          new UpdateStagingLocationComboListener(stagingLocationsFuture);
      stagingLocationsFuture.addListener(updateComboListener, executor);
    }
  }

  /**
   * Ensure the staging location specified in the input combo is valid.
   */
  private void verifyStagingLocation(final String stagingLocation) {
    if (verifyJob != null) {
      // Cancel any existing verifyJob
      verifyJob.cancel();
    }
    if (client == null) {
      // We can't verify the staging locations because we don't have a GCS client
      return;
    }
    if (stagingLocation.isEmpty()) {
      // If the staging location is empty, we don't have anything to verify; and we don't have any
      // interesting messaging
      messageTarget.clear();
      return;
    }
    verifyJob = VerifyStagingLocationJob.create(client, stagingLocation);
    verifyJob.schedule(VERIFY_LOCATION_DELAY_MS);
    final ListenableFutureProxy<VerifyStagingLocationResult> resultFuture =
        verifyJob.getVerifyResult();
    resultFuture.addListener(
        new Runnable() {
          @Override
          public void run() {
            try {
              VerifyStagingLocationResult result = resultFuture.get();
              if (result.getStagingLocation().equals(stagingLocationInput.getText())
                  && result.isAccessible()) {
                messageTarget.setInfo("Found staging location " + stagingLocation);
                createButton.setEnabled(false);
              } else {
                messageTarget.setError(String.format("Couldn't fetch bucket %s", stagingLocation));
                createButton.setEnabled(true);
              }
            } catch (InterruptedException | ExecutionException e) {
              messageTarget.setError(String.format("Couldn't fetch bucket %s", stagingLocation));
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
      if (client == null) {
        return;
      }
      String projectName = projectInput.getText();
      String stagingLocation = stagingLocationInput.getText();
      client.createStagingLocation(projectName, stagingLocation, new NullProgressMonitor());
      messageTarget.setInfo("Created staging location at " + stagingLocation);
    }
  }

  /**
   * A job change listener that updates a Combo with a collection of retrieved staging locations
   * when the listened job completes.
   */
  private class UpdateStagingLocationComboListener implements Runnable {
    private final Future<SortedSet<String>> stagingLocationsFuture;

    public UpdateStagingLocationComboListener(Future<SortedSet<String>> stagingLocationsFuture) {
      this.stagingLocationsFuture = stagingLocationsFuture;
    }

    /**
     * Update the Combo with the staging locations retrieved by the Job.
     */
    @Override
    public void run() {
      SortedSet<String> stagingLocations;
      try {
        stagingLocations = stagingLocationsFuture.get();
      } catch (InterruptedException | ExecutionException e) {
        messageTarget.setError("Could not retrieve buckets for project " + projectInput.getText());
        DataflowUiPlugin.logError(e, "Exception while retrieving potential staging locations");
        return;
      }
      messageTarget.clear();
      String currentValue = stagingLocationInput.getText();
      Point currentSelection = stagingLocationInput.getSelection();
      int startSelection = currentSelection.x;
      int endSelection = currentSelection.y;
      stagingLocationInput.removeAll();
      for (String location : stagingLocations) {
        stagingLocationInput.add(location);
      }
      stagingLocationInput.setText(currentValue);
      completionListener.setContents(stagingLocations);
      stagingLocationInput.setSelection(new Point(startSelection, endSelection));
    }
  }
  
  private class EnableCreateButton implements ModifyListener {

    @Override
    public void modifyText(ModifyEvent event) {
      boolean enabled = !stagingLocationInput.getText().trim().isEmpty();
      createButton.setEnabled(enabled);
    }

  }
}
