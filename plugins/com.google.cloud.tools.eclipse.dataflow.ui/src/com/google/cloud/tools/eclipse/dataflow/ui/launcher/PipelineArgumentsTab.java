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

package com.google.cloud.tools.eclipse.dataflow.ui.launcher;

import com.google.cloud.tools.eclipse.dataflow.core.launcher.ClasspathPipelineOptionsHierarchyFactory;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.PipelineLaunchConfiguration;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.PipelineLaunchConfiguration.MissingRequiredProperties;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.PipelineOptionsHierarchyFactory;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.PipelineOptionsRetrievalException;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.PipelineRunner;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsHierarchy;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsProperty;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsType;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.ProjectOrWorkspaceDataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowDependencyManager;
import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.cloud.tools.eclipse.dataflow.ui.DataflowUiPlugin;
import com.google.cloud.tools.eclipse.dataflow.ui.page.MessageTarget;
import com.google.cloud.tools.eclipse.dataflow.ui.page.component.LabeledTextMapComponent;
import com.google.cloud.tools.eclipse.dataflow.ui.page.component.TextAndButtonComponent;
import com.google.cloud.tools.eclipse.dataflow.ui.page.component.TextAndButtonSelectionListener;
import com.google.cloud.tools.eclipse.dataflow.ui.util.DisplayExecutor;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.SettableFuture;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;

/**
 * A tab specifying arguments required to run a Dataflow Pipeline.
 */
public class PipelineArgumentsTab extends AbstractLaunchConfigurationTab {
  private static final Joiner MISSING_GROUP_MEMBER_JOINER = Joiner.on(", ");

  private static final String ARGUMENTS_SEPARATOR = "=";

  private Executor executor;

  private ScrolledComposite composite;
  private Composite internalComposite;

  private Group runnerGroup;
  private Map<PipelineRunner, Button> runnerButtons;

  private DefaultedPipelineOptionsComponent defaultOptionsComponent;

  private TextAndButtonComponent userOptionsSelector;
  private PipelineOptionsFormComponent pipelineOptionsForm;

  private PipelineLaunchConfiguration launchConfiguration;

  private final DataflowDependencyManager dependencyManager;
  private final PipelineOptionsHierarchyFactory pipelineOptionsHierarchyFactory;

  /*
   * TODO: By default, this may include all PipelineOptions types, including custom user types that
   * are not present in the project that this PipelineArgumentsTab is trying to launch. This
   * hierarchy should be restricted to only showing options available from the current project, if
   * able.
   */
  private PipelineOptionsHierarchy hierarchy;

  private IWorkspaceRoot workspaceRoot;
  private Job job;

  public PipelineArgumentsTab() {
    this(
        DataflowDependencyManager.create(),
        new ClasspathPipelineOptionsHierarchyFactory(),
        ResourcesPlugin.getWorkspace().getRoot());
  }

  private PipelineArgumentsTab(
      DataflowDependencyManager dependencyManager,
      PipelineOptionsHierarchyFactory retrieverFactory,
      IWorkspaceRoot workspaceRoot) {
    this.dependencyManager = dependencyManager;
    this.pipelineOptionsHierarchyFactory = retrieverFactory;
    this.workspaceRoot = workspaceRoot;
    hierarchy = retrieverFactory.global(new NullProgressMonitor());
  }

  @Override
  public void createControl(Composite parent) {
    launchConfiguration = PipelineLaunchConfiguration.createDefault();
    executor = DisplayExecutor.create(parent.getDisplay());
    composite = new ScrolledComposite(parent, SWT.V_SCROLL);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    composite.setLayout(new GridLayout(1, false));

    internalComposite = new Composite(this.composite, SWT.NULL);

    GridData internalCompositeGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    internalComposite.setLayoutData(internalCompositeGridData);
    internalComposite.setLayout(new GridLayout(1, false));

    runnerGroup = new Group(internalComposite, SWT.NULL);
    runnerGroup.setText("Runner:");
    runnerGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    runnerGroup.setLayout(new GridLayout(2, false));

    createDefaultOptionsComponent(internalComposite, new GridData(SWT.FILL, SWT.FILL, true, false));

    Composite inputsComposite = new Composite(internalComposite, SWT.NULL);
    inputsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    inputsComposite.setLayout(new FillLayout(SWT.VERTICAL));

    Set<String> filterProperties =
        ImmutableSet.<String>builder()
            .addAll(DataflowPreferences.SUPPORTED_DEFAULT_PROPERTIES)
            .add("runner")
            .build();

    Group runnerOptionsGroup = new Group(inputsComposite, SWT.NULL);
    runnerOptionsGroup.setText("Pipeline Options:");
    runnerOptionsGroup.setLayout(new GridLayout());

    userOptionsSelector = new TextAndButtonComponent(
        runnerOptionsGroup, new GridData(SWT.FILL, SWT.BEGINNING, true, false), "&Search...");
    userOptionsSelector.addButtonSelectionListener(openPipelineOptionsSearchListener());

    pipelineOptionsForm =
        new PipelineOptionsFormComponent(runnerOptionsGroup, ARGUMENTS_SEPARATOR, filterProperties);
    pipelineOptionsForm.addModifyListener(new UpdateLaunchConfigurationDialogTextChangedListener());
    pipelineOptionsForm.addExpandListener(new UpdateLaunchConfigurationDialogExpandListener());

    composite.setContent(internalComposite);
    composite.setExpandHorizontal(true);
    composite.setExpandVertical(true);
    composite.setMinSize(inputsComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    composite.setShowFocusedControl(true);
    composite.pack(true);

    setControl(composite);
  }

  private TextAndButtonSelectionListener openPipelineOptionsSearchListener() {
    return new TextAndButtonSelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        Map<String, PipelineOptionsType> optionsTypes = hierarchy.getAllPipelineOptionsTypes();
        PipelineOptionsSelectionDialog dialog =
            new PipelineOptionsSelectionDialog(getShell(), optionsTypes);
        dialog.setBlockOnOpen(true);
        dialog.setInitialPattern("**");
        if (dialog.open() == Window.OK) {
          String userOptionsName = dialog.getFirstResult().toString();
          setTextValue(userOptionsName);
          launchConfiguration.setUserOptionsName(userOptionsName);
        }
        updatePipelineOptionsForm();
        return;
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {}
    };
  }

  private void populateRunners(MajorVersion majorVersion) {
    for (Control ctl : runnerGroup.getChildren()) {
      ctl.dispose();
    }
    runnerButtons = new HashMap<>();
    // TODO: Retrieve automatically instead of from a hardcoded map
    for (PipelineRunner runner : PipelineRunner.inMajorVersion(majorVersion)) {
      Button runnerButton = createRunnerButton(runnerGroup, runner);
      runnerButton.addSelectionListener(
          new UpdateLaunchConfigAndRequiredArgsSelectionListener(runner));
      runnerButtons.put(runner, runnerButton);
    }
  }

  private Button createRunnerButton(Group runnerSelectorGroup, PipelineRunner runner) {
    Button radio = new Button(runnerSelectorGroup, SWT.RADIO);
    radio.setText(runner.getRunnerName());
    radio.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    Label description = new Label(runnerSelectorGroup, SWT.NULL);
    description.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    description.setText(runner.getDescription());
    return radio;
  }

  private void createDefaultOptionsComponent(Composite composite, GridData layoutData) {
    MessageTarget target = new MessageTarget() {
      @Override
      public void setInfo(String message) {
        setMessage(message);
      }

      @Override
      public void setError(String message) {
        setErrorMessage(message);
      }

      @Override
      public void clear() {
        setErrorMessage(null);
        setMessage(null);
      }
    };

    defaultOptionsComponent =
        new DefaultedPipelineOptionsComponent(composite, layoutData, target, getPreferences());

    defaultOptionsComponent.addButtonSelectionListener(
        new UpdateLaunchConfigurationDialogSelectionListener());
    defaultOptionsComponent.addModifyListener(
        new UpdateLaunchConfigurationDialogTextChangedListener());
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    launchConfiguration = PipelineLaunchConfiguration.createDefault();
    launchConfiguration.toLaunchConfiguration(configuration);
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    PipelineRunner runner = getSelectedRunner();
    launchConfiguration.setRunner(runner);

    launchConfiguration.setUseDefaultLaunchOptions(defaultOptionsComponent.isUseDefaultOptions());

    Map<String, String> overallArgValues = new HashMap<>(launchConfiguration.getArgumentValues());
    if (!defaultOptionsComponent.isUseDefaultOptions()) {
      overallArgValues.putAll(defaultOptionsComponent.getValues());
    }

    overallArgValues.putAll(getNonDefaultOptions());
    launchConfiguration.setUserOptionsName(userOptionsSelector.getText());

    launchConfiguration.setArgumentValues(overallArgValues);

    launchConfiguration.toLaunchConfiguration(configuration);
  }

  private PipelineRunner getSelectedRunner() {
    for (Map.Entry<PipelineRunner, Button> runnerButton : runnerButtons.entrySet()) {
      if (runnerButton.getValue().getSelection()) {
        return runnerButton.getKey();
      }
    }
    throw new IllegalStateException("No runner selected, but a runner starts selected");
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    try {
      launchConfiguration = PipelineLaunchConfiguration.fromLaunchConfiguration(configuration);

      MajorVersion majorVersion = dependencyManager.getProjectMajorVersion(getProject());
      if (majorVersion == null) {
        majorVersion = MajorVersion.ONE;
      }
      updateRunnerButtons(majorVersion);
      updateHierarchy(majorVersion);

      defaultOptionsComponent.setUseDefaultValues(launchConfiguration.isUseDefaultLaunchOptions());
      defaultOptionsComponent.setPreferences(getPreferences());
      defaultOptionsComponent.setCustomValues(launchConfiguration.getArgumentValues());

      String userOptionsName = launchConfiguration.getUserOptionsName();
      userOptionsSelector.setText(userOptionsName == null ? "" : userOptionsName);

      updatePipelineOptionsForm();
    } catch (CoreException e) {
      // TODO: Handle
      DataflowUiPlugin.logError(e, "Error while initializing from existing configuration");
    }
  }

  private void updateRunnerButtons(MajorVersion majorVersion) {
    populateRunners(majorVersion);
    for (Button button : runnerButtons.values()) {
      button.setSelection(false);
    }

    PipelineRunner runner = launchConfiguration.getRunner();
    Button runnerButton = runnerButtons.get(runner);
    if (runnerButton == null) {
      runnerButtons
          .get(PipelineLaunchConfiguration.defaultRunner(majorVersion))
          .setSelection(true);
    } else {
      runnerButton.setSelection(true);
    }
    runnerGroup.getParent().redraw();
  }

  /**
   * Asynchronously updates the project hierarchy.
   */
  private void updateHierarchy(final MajorVersion majorVersion) {
    Job job = new Job("Update Hierarchy") {
      @Override
      public IStatus run(IProgressMonitor progress) {
        hierarchy = getPipelineOptionsHierarchy(majorVersion, progress);
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  private DataflowPreferences getPreferences() {
    IProject project = getProject();
    if (project != null && project.isAccessible()) {
      return ProjectOrWorkspaceDataflowPreferences.forProject(project);
    } else {
      return ProjectOrWorkspaceDataflowPreferences.forWorkspace();
    }
  }

  private PipelineOptionsHierarchy getPipelineOptionsHierarchy(
      MajorVersion majorVersion, IProgressMonitor monitor) {
    IProject project = getProject();
    if (project != null && project.isAccessible()) {
      try {
        return pipelineOptionsHierarchyFactory.forProject(project, majorVersion, monitor);
      } catch (PipelineOptionsRetrievalException e) {
        DataflowUiPlugin.logWarning(
            "Couldn't retrieve Pipeline Options Hierarchy for project %s", project);
        return pipelineOptionsHierarchyFactory.global(monitor);
      }
    }
    return pipelineOptionsHierarchyFactory.global(monitor);
  }

  private IProject getProject() {
    String eclipseProjectName = launchConfiguration.getEclipseProjectName();
    if (eclipseProjectName != null) {
      return workspaceRoot.getProject(eclipseProjectName);
    }
    return null;
  }

  @Override
  public String getName() {
    return "Pipeline Arguments";
  }

  private void updatePipelineOptionsForm() {
    final SettableFuture<Map<PipelineOptionsType, Set<PipelineOptionsProperty>>>
        optionsHierarchyFuture = SettableFuture.create();
    Job job = new Job("Update Pipeline Options Form") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        optionsHierarchyFuture.set(launchConfiguration.getOptionsHierarchy(hierarchy));
        return Status.OK_STATUS;
      }
    };
    job.schedule();
    optionsHierarchyFuture.addListener(
        new Runnable() {
          @Override
          public void run() {
            try {
              pipelineOptionsForm.updateForm(launchConfiguration, optionsHierarchyFuture.get());
              updateLaunchConfigurationDialog();
            } catch (InterruptedException | ExecutionException e) {
              DataflowUiPlugin.logError(e, "Exception while updating available Pipeline Options");
            }
          }
        },
        executor);
  }

  private Map<String, String> getNonDefaultOptions() {
    Map<String, String> argumentValues = new HashMap<>();
    for (LabeledTextMapComponent optionsComponent : pipelineOptionsForm.getComponents()) {
      argumentValues.putAll(optionsComponent.getTextValues());
    }
    return argumentValues;
  }

  private boolean validatePage() {
    MissingRequiredProperties validationFailures =
        launchConfiguration.getMissingRequiredProperties(hierarchy, getPreferences());

    setErrorMessage(null);
    return validateRequiredProperties(validationFailures)
        && validateRequiredGroups(validationFailures);
  }

  private boolean validateRequiredGroups(MissingRequiredProperties validationFailures) {
    if (!validationFailures.getMissingGroups().isEmpty()) {
      Map.Entry<String, Set<PipelineOptionsProperty>> missingGroupEntry =
          Iterables.getFirst(validationFailures.getMissingGroups().entrySet(), null);
      StringBuilder errorBuilder = new StringBuilder("Missing value for group ");
      errorBuilder.append(missingGroupEntry.getKey());
      errorBuilder.append(". Properties satisfying group requirement are ");
      Set<String> groupMembers = new HashSet<>();
      for (PipelineOptionsProperty missingProperty : missingGroupEntry.getValue()) {
        groupMembers.add(missingProperty.getName());
      }
      errorBuilder.append(MISSING_GROUP_MEMBER_JOINER.join(groupMembers));
      errorBuilder.append(".");
      setErrorMessage(errorBuilder.toString());
      return false;
    }
    return true;
  }

  private boolean validateRequiredProperties(MissingRequiredProperties validationFailures) {
    if (!validationFailures.getMissingProperties().isEmpty()) {
      PipelineOptionsProperty missingProperty =
          Iterables.getFirst(validationFailures.getMissingProperties(), null);
      setErrorMessage("Missing required property " + missingProperty.getName());
      return false;
    }
    return true;
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    return validatePage();
  }

  @Override
  protected void updateLaunchConfigurationDialog() {
    composite.setMinSize(internalComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    composite.pack();
    super.updateLaunchConfigurationDialog();
  }

  /**
   * When the Runner selectino is changed, update the underlying launch configuration, update the
   * PipelineOptionsForm to show all available inputs, and rerender the tab.
   */
  private class UpdateLaunchConfigAndRequiredArgsSelectionListener extends SelectionAdapter {
    private final PipelineRunner runner;

    public UpdateLaunchConfigAndRequiredArgsSelectionListener(PipelineRunner runner) {
      this.runner = runner;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
      launchConfiguration.setRunner(runner);
      updatePipelineOptionsForm();
      updateLaunchConfigurationDialog();
    }
  }

  /**
   * When a launch configuration property changes, ensure the validation state is reflected in the
   * arguments tab.
   */
  private class UpdateLaunchConfigurationDialogTextChangedListener implements ModifyListener {
    @Override
    public void modifyText(ModifyEvent e) {
      updateLaunchConfigurationDialog();
    }
  }

  /**
   * When the default options button is selected, ensure the validation state is refelected in the
   * arguments tab.
   */
  private class UpdateLaunchConfigurationDialogSelectionListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      updateLaunchConfigurationDialog();
    }
  }

  /**
   * Whenever a {@link PipelineOptionsType} header is expanded, ensure the min size of the {@code
   * ScrolledComposite} is updated to fit the entire form.
   */
  private class UpdateLaunchConfigurationDialogExpandListener implements IExpansionListener {
    @Override
    public void expansionStateChanging(ExpansionEvent e) {}

    @Override
    public void expansionStateChanged(ExpansionEvent e) {
      updateLaunchConfigurationDialog();
    }
  }
}
