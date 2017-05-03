/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.deploy.DeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.internal.ProjectSelectorSelectionChangedListener;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.login.ui.AccountSelectorObservableValue;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.ui.util.FontUtil;
import com.google.cloud.tools.eclipse.ui.util.databinding.BucketNameValidator;
import com.google.cloud.tools.eclipse.ui.util.databinding.ProjectVersionValidator;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.ErrorDialogErrorHandler;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.QueryParameterProvider;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.ObservablesManager;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.osgi.service.prefs.BackingStoreException;

public abstract class AppEngineDeployPreferencesPanel extends DeployPreferencesPanel {

  private static final String APPENGINE_VERSIONS_URL =
      "https://console.cloud.google.com/appengine/versions";
  private static final String CREATE_GCP_PROJECT_WITH_GAE_URL =
      "https://console.cloud.google.com/projectselector/appengine/create?lang=java";

  private static final Logger logger = Logger.getLogger(
      AppEngineDeployPreferencesPanel.class.getName());

  private AccountSelector accountSelector;

  private ProjectSelector projectSelector;

  private Text version;

  private Button autoPromoteButton;

  private Button stopPreviousVersionButton;

  private Text bucket;

  private ExpandableComposite expandableComposite;

  private final Image refreshIcon = SharedImages.REFRESH_IMAGE_DESCRIPTOR.createImage(getDisplay());

  protected final IProject project;
  protected final DeployPreferences model;
  private final ObservablesManager observables = new ObservablesManager();
  protected final DataBindingContext bindingContext = new DataBindingContext();

  private final Runnable layoutChangedHandler;
  protected final boolean requireValues;

  private final ProjectRepository projectRepository;
  private final FormToolkit formToolkit;

  public AppEngineDeployPreferencesPanel(Composite parent, IProject project,
      IGoogleLoginService loginService, Runnable layoutChangedHandler, boolean requireValues,
      ProjectRepository projectRepository, DeployPreferences model) {
    super(parent, SWT.NONE);

    this.project = project;
    this.layoutChangedHandler = Preconditions.checkNotNull(layoutChangedHandler);
    this.requireValues = requireValues;
    this.projectRepository = projectRepository;
    this.model = model;

    FormColors colors = new FormColors(getDisplay());
    colors.setBackground(null);
    colors.setForeground(null);
    formToolkit = new FormToolkit(colors);

    createCredentialSection(loginService);
    createProjectIdSection();
    setupAccountEmailDataBinding();
    setupProjectSelectorDataBinding();

    createCenterArea();

    createAdvancedSection();
    setupTextFieldDataBinding(bucket, "bucket", new BucketNameValidator());

    observables.addObservablesFromContext(bindingContext, true, true);

    Dialog.applyDialogFont(this);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(this);
  }

  protected void createCenterArea() {
    createProjectVersionSection();
    setupTextFieldDataBinding(version, "version", new ProjectVersionValidator());

    createPromoteSection();
    setupMasterDependantDataBinding(autoPromoteButton, "autoPromote",
        stopPreviousVersionButton, "stopPreviousVersion");
  }

  private void setupAccountEmailDataBinding() {
    AccountSelectorObservableValue accountSelectorObservableValue =
        new AccountSelectorObservableValue(accountSelector);
    UpdateValueStrategy modelToTarget =
        new UpdateValueStrategy().setConverter(new Converter(String.class, String.class) {
          @Override
          public Object convert(Object savedEmail) {
            Preconditions.checkArgument(savedEmail instanceof String);
            if (accountSelector.isEmailAvailable((String) savedEmail)) {
              return savedEmail;
            } else if (requireValues && accountSelector.getAccountCount() == 1) {
              return accountSelector.getFirstEmail();
            } else {
              return null;
            }
          }
        });

    final IObservableValue accountEmailModel = PojoProperties.value("accountEmail").observe(model);

    Binding binding = bindingContext.bindValue(accountSelectorObservableValue, accountEmailModel,
        new UpdateValueStrategy(), modelToTarget);
    /*
     * Trigger an explicit target -> model update for the auto-select-single-account case. When the
     * model has a null account but there is exactly 1 login account, then the AccountSelector
     * automatically selects that account. That change means the AccountSelector is at odds with the
     * model.
     */
    binding.updateTargetToModel();
    bindingContext.addValidationStatusProvider(new AccountSelectorValidator(
        requireValues, accountSelector, accountSelectorObservableValue));
  }

  private void setupProjectSelectorDataBinding() {
    IViewerObservableValue projectInput =
        ViewerProperties.input().observe(projectSelector.getViewer());
    IViewerObservableValue projectSelection =
        ViewerProperties.singleSelection().observe(projectSelector.getViewer());
    bindingContext.addValidationStatusProvider(
        new ProjectSelectionValidator(projectInput, projectSelection, requireValues));

    IViewerObservableValue projectList =
        ViewerProperties.singleSelection().observe(projectSelector.getViewer());
    IObservableValue projectIdModel = PojoProperties.value("projectId").observe(model);

    UpdateValueStrategy gcpProjectToProjectId =
        new UpdateValueStrategy().setConverter(new GcpProjectToProjectIdConverter());
    UpdateValueStrategy projectIdToGcpProject =
        new UpdateValueStrategy().setConverter(new ProjectIdToGcpProjectConverter());

    bindingContext.bindValue(projectList, projectIdModel,
        gcpProjectToProjectId, projectIdToGcpProject);
  }

  private void setupTextFieldDataBinding(Control control, String modelPropertyName,
      IValidator setAfterGetValidator) {
    ISWTObservableValue controlValue = WidgetProperties.text(SWT.Modify).observe(control);
    IObservableValue modelValue = PojoProperties.value(modelPropertyName).observe(model);

    bindingContext.bindValue(controlValue, modelValue,
        new UpdateValueStrategy().setAfterGetValidator(setAfterGetValidator),
        new UpdateValueStrategy().setAfterGetValidator(setAfterGetValidator));
  }

  /**
   * <ul>
   *   <li> Binds {@code master} to the property with the name {@code masterModelPropertyName}
   *       in the {@link #model}.
   *   <li> Binds {@code dependant} to the property with the name {@code dependantModelPropertyName}
   *       in the {#link model}.
   *   <li> Binds {@code master} and {@code dependant} in a way that {@code dependant} is disabled
   *       and unchecked when {@code master} is unchecked. When {@code master} is checked back,
   *       {@code dependant} restores its previous check state.
   * <ul>
   */
  private void setupMasterDependantDataBinding(Control master, String masterModelPropertyName,
      Control dependant, String dependantModelPropertyName) {

    ISWTObservableValue masterValue = WidgetProperties.selection().observe(master);
    final ISWTObservableValue dependantValue = WidgetProperties.selection().observe(dependant);
    final ISWTObservableValue dependantEnablement = WidgetProperties.enabled().observe(dependant);

    IObservableValue masterModel = PojoProperties.value(masterModelPropertyName).observe(model);
    IObservableValue dependantModel =
        PojoProperties.value(dependantModelPropertyName).observe(model);

    bindingContext.bindValue(dependantEnablement, masterValue);

    bindingContext.bindValue(masterValue, masterModel);

    // Intermediary model necessary for "Restore Defaults" to work.
    final IObservableValue currentDependantChoice = new WritableValue();
    bindingContext.bindValue(currentDependantChoice, dependantModel);

    // One-way update: button selection <-- latest user choice
    // Update the button (to match the user choice), if enabled; if not, force unchecking.
    bindingContext.bindValue(dependantValue, new ComputedValue() {
      @Override
      protected Object calculate() {
        boolean controlEnabled = (boolean) dependantEnablement.getValue();
        boolean currentValue = (boolean) currentDependantChoice.getValue();
        if (!controlEnabled) {
          return Boolean.FALSE;  // Force unchecking the stop previous button if it is disabled.
        }
        return currentValue;  // Otherwise, check the button according to the latest user choice.
      }
    }, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER), null);

    // One-way update: button selection --> latest user choice
    // Update the user choice (to match the button selection), only when the button is enabled.
    bindingContext.bindValue(new ComputedValue() {
      @Override
      protected Object calculate() {
        boolean controlEnabled = (boolean) dependantEnablement.getValue();
        boolean controlValue = (boolean) dependantValue.getValue();
        boolean currentValue = (boolean) currentDependantChoice.getValue();
        if (controlEnabled) {
          return controlValue;  // Remember the button state as the latest choice if it is enabled.
        }
        return currentValue;  // Otherwise, retain the latest (current) user choice.
      }
    }, dependantModel, null, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER));
  }

  protected void setupCheckBoxDataBinding(Button button, String modelPropertyName) {
    ISWTObservableValue buttonValue = WidgetProperties.selection().observe(button);
    IObservableValue modelValue = PojoProperties.value(modelPropertyName).observe(model);
    bindingContext.bindValue(buttonValue, modelValue);
  }

  @Override
  boolean savePreferences() {
    try {
      model.save();
      return true;
    } catch (BackingStoreException exception) {
      logger.log(Level.SEVERE, "Could not save deploy preferences", exception);
      MessageDialog.openError(getShell(),
                              Messages.getString("deploy.preferences.save.error.title"),
                              Messages.getString("deploy.preferences.save.error.message",
                                                 exception.getLocalizedMessage()));
      return false;
    }
  }

  public Credential getSelectedCredential() {
    return accountSelector.getSelectedCredential();
  }

  private void createCredentialSection(IGoogleLoginService loginService) {
    Label accountLabel = new Label(this, SWT.LEAD);
    accountLabel.setText(Messages.getString("deploy.preferences.dialog.label.selectAccount"));
    accountLabel.setToolTipText(Messages.getString("tooltip.account"));

    accountSelector = new AccountSelector(this, loginService,
        Messages.getString("deploy.preferences.dialog.accountSelector.login"));
    accountSelector.setToolTipText(Messages.getString("tooltip.account"));
    GridData accountSelectorGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    accountSelector.setLayoutData(accountSelectorGridData);
  }

  private void createProjectIdSection() {
    Label projectIdLabel = new Label(this, SWT.LEAD);
    projectIdLabel.setText(Messages.getString("project"));
    projectIdLabel.setToolTipText(Messages.getString("tooltip.project.id"));
    GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.BEGINNING).span(1, 2)
        .applyTo(projectIdLabel);

    Composite linkComposite = new Composite(this, SWT.NONE);
    Link createNewProject = new Link(linkComposite, SWT.WRAP);
    createNewProject.setText(Messages.getString("projectselector.createproject",
                                                CREATE_GCP_PROJECT_WITH_GAE_URL));
    createNewProject.setToolTipText(Messages.getString("projectselector.createproject.tooltip"));
    FontUtil.convertFontToItalic(createNewProject);
    createNewProject.addSelectionListener(
        new OpenUriSelectionListener(new QueryParameterProvider() {
          @Override
          public Map<String, String> getParameters() {
            if (accountSelector.getSelectedEmail().isEmpty()) {
              return Collections.emptyMap();
            } else {
              return Collections.singletonMap("authuser", accountSelector.getSelectedEmail());
            }
          }
        }, new ErrorDialogErrorHandler(getShell())));
    GridDataFactory.fillDefaults().applyTo(linkComposite);
    GridLayoutFactory.fillDefaults().generateLayout(linkComposite);

    Composite projectSelectorComposite = new Composite(this, SWT.NONE);
    GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).applyTo(projectSelectorComposite);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(projectSelectorComposite);

    projectSelector = new ProjectSelector(projectSelectorComposite);
    GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 200)
        .applyTo(projectSelector);

    final Button refreshProjectsButton = new Button(projectSelectorComposite, SWT.NONE);
    refreshProjectsButton.setImage(refreshIcon);
    GridDataFactory.swtDefaults().align(SWT.END, SWT.BEGINNING).applyTo(refreshProjectsButton);
    refreshProjectsButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        refreshProjectsForSelectedCredential();
      }
    });

    accountSelector.addSelectionListener(
        new RefreshProjectOnAccountSelection(refreshProjectsButton));

    projectSelector.addSelectionChangedListener(
        new ProjectSelectorSelectionChangedListener(accountSelector,
                                                    projectRepository,
                                                    projectSelector));
  }

  private void createProjectVersionSection() {
    Label versionLabel = new Label(this, SWT.LEAD);
    versionLabel.setText(Messages.getString("custom.versioning"));
    versionLabel.setToolTipText(Messages.getString("tooltip.version"));

    version = new Text(this, SWT.LEAD | SWT.SINGLE | SWT.BORDER);
    version.setMessage(Messages.getString("custom.versioning.hint"));
    version.setToolTipText(Messages.getString("tooltip.version"));
    GridData versionGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    version.setLayoutData(versionGridData);
  }

  private void createPromoteSection() {
    autoPromoteButton = createCheckBox(Messages.getString("auto.promote"),
        Messages.getString("tooltip.manual.promote.link", APPENGINE_VERSIONS_URL));
    stopPreviousVersionButton = createCheckBox(
        Messages.getString("stop.previous.version"),
        Messages.getString("tooltip.stop.previous.version"));
  }

  protected Button createCheckBox(String text, String tooltip) {
    Button checkBox = new Button(this, SWT.CHECK);
    checkBox.setText(text);
    checkBox.setToolTipText(tooltip);

    GridData gridData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
    gridData.horizontalSpan = 2;
    checkBox.setLayoutData(gridData);
    return checkBox;
  }

  private void createAdvancedSection() {
    createExpandableComposite();
    final Composite bucketComposite = createBucketSection(expandableComposite);

    expandableComposite.setClient(bucketComposite);
    expandableComposite.addExpansionListener(new ExpansionAdapter() {
      @Override
      public void expansionStateChanged(ExpansionEvent event) {
        layoutChangedHandler.run();
      }
    });
  }

  private void createExpandableComposite() {
    expandableComposite = new ExpandableComposite(this, SWT.NONE, ExpandableComposite.TWISTIE);
    FontUtil.convertFontToBold(expandableComposite);
    expandableComposite.setText(Messages.getString("settings.advanced"));
    expandableComposite.setExpanded(false);
    GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gridData.horizontalSpan = 2;
    expandableComposite.setLayoutData(gridData);

    formToolkit.adapt(expandableComposite, true, true);
  }

  private Composite createBucketSection(Composite parent) {
    Composite bucketComposite = new Composite(parent, SWT.NONE);

    Label bucketLabel = new Label(bucketComposite, SWT.LEAD);
    bucketLabel.setText(Messages.getString("custom.bucket"));
    bucketLabel.setToolTipText(Messages.getString("tooltip.staging.bucket"));

    bucket = new Text(bucketComposite, SWT.LEAD | SWT.SINGLE | SWT.BORDER);
    bucket.setMessage(Messages.getString("custom.bucket.hint"));
    GridData bucketData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    bucket.setLayoutData(bucketData);

    bucket.setToolTipText(Messages.getString("tooltip.staging.bucket"));

    GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(bucketComposite);
    return bucketComposite;
  }

  @VisibleForTesting
  public Job latestGcpProjectQueryJob;  // Must be updated/accessed in the UI context.

  private Predicate<Job> isLatestQueryJob = new Predicate<Job>() {
    @Override
    public boolean apply(Job job) {
      return job == latestGcpProjectQueryJob;
    }
  };

  private void refreshProjectsForSelectedCredential() {
    projectSelector.setProjects(Collections.<GcpProject>emptyList());
    latestGcpProjectQueryJob = null;

    Credential selectedCredential = accountSelector.getSelectedCredential();
    if (selectedCredential != null) {
      latestGcpProjectQueryJob = new GcpProjectQueryJob(selectedCredential,
          projectRepository, projectSelector, bindingContext, isLatestQueryJob);
      latestGcpProjectQueryJob.schedule();
    }
  }

  private final class RefreshProjectOnAccountSelection implements Runnable {

    private final Button refreshProjectsButton;

    public RefreshProjectOnAccountSelection(Button refreshProjectsButton) {
      this.refreshProjectsButton = refreshProjectsButton;
    }

    @Override
    public void run() {
      refreshProjectsForSelectedCredential();
      refreshProjectsButton.setEnabled(accountSelector.getSelectedCredential() != null);
    }
  }

  private final class ProjectIdToGcpProjectConverter extends Converter {

    private ProjectIdToGcpProjectConverter() {
      super(String.class, GcpProject.class);
    }

    @Override
    public Object convert(Object fromObject) {
      if (fromObject == null) {
        return null;
      }

      Preconditions.checkArgument(fromObject instanceof String);
      try {
        return projectRepository.getProject(accountSelector.getSelectedCredential(),
                                           (String) fromObject);
      } catch (ProjectRepositoryException ex) {
        return null;
      }
    }
  }

  private final class GcpProjectToProjectIdConverter extends Converter {

    private GcpProjectToProjectIdConverter() {
      super(GcpProject.class, String.class);
    }

    @Override
    public Object convert(Object fromObject) {
      if (fromObject == null) {
        return null;
      }

      Preconditions.checkArgument(fromObject instanceof GcpProject);
      return ((GcpProject) fromObject).getId();
    }
  }

  static class ProjectSelectionValidator extends FixedMultiValidator {

    private final IViewerObservableValue projectInput;
    private final IViewerObservableValue projectSelection;
    private final boolean requireValues;

    private ProjectSelectionValidator(IViewerObservableValue projectInput,
                                      IViewerObservableValue projectSelection,
                                      boolean requireValues) {
      this.projectInput = projectInput;
      this.projectSelection = projectSelection;
      this.requireValues = requireValues;
    }

    @Override
    protected IStatus validate() {
      // this access is recorded and ensures that changes are tracked, don't move it inside the if
      Collection<?> projects = (Collection<?>) projectInput.getValue();
      // this access is recorded and ensures that changes are tracked, don't move it inside the if
      Object selectedProject = projectSelection.getValue();
      if (projects.isEmpty()) {
        if (requireValues) {
          return ValidationStatus.error(Messages.getString("projectselector.no.projects")); //$NON-NLS-1$
        } else {
          return ValidationStatus.info(Messages.getString("projectselector.no.projects")); //$NON-NLS-1$
        }
      }
      if (requireValues) {
        if (selectedProject == null) {
          return ValidationStatus.error(Messages.getString("projectselector.project.not.selected")); //$NON-NLS-1$
        }
      }
      return ValidationStatus.ok();
    }
  }

  /**
   * Validates the {@link AccountSelector account selector} state against the panel settings.
   * Reports an error if the panel requires all values to be set, but the account selector does not
   * have a valid account.
   */
  private static class AccountSelectorValidator extends FixedMultiValidator {
    final private boolean requireValues;
    final private AccountSelectorObservableValue accountSelectorObservableValue;
    final private AccountSelector accountSelector;

    private AccountSelectorValidator(boolean requireValues, AccountSelector accountSelector,
        AccountSelectorObservableValue accountSelectorObservableValue) {
      this.requireValues = requireValues;
      this.accountSelector = accountSelector;
      this.accountSelectorObservableValue = accountSelectorObservableValue;
      // trigger the validator, as defaults to OK otherwise
      getValidationStatus();
    }

    @Override
    protected IStatus validate() {
      // access accountSelectorObservableValue so MultiValidator records the access
      String selectedEmail = (String) accountSelectorObservableValue.getValue();
      if (requireValues && Strings.isNullOrEmpty(selectedEmail)) {
        if (accountSelector.isSignedIn()) {
          return ValidationStatus.error(Messages.getString("error.account.missing.signedin"));
        } else {
          return ValidationStatus.error(Messages.getString("error.account.missing.signedout"));
        }
      }
      return ValidationStatus.ok();
    }
  }

  // BUGFIX: https://bugs.eclipse.org/bugs/show_bug.cgi?id=312785
  private abstract static class FixedMultiValidator extends MultiValidator {
    @Override
    public IObservableList getTargets() {
      if (isDisposed()) {
        return Observables.emptyObservableList();
      }
      return super.getTargets();
    }
  }

  @VisibleForTesting
  @Override
  public DataBindingContext getDataBindingContext() {
    return bindingContext;
  }

  @Override
  void resetToDefaults() {
    model.resetToDefaults();
    bindingContext.updateTargets();
  }

  @Override
  public void dispose() {
    formToolkit.dispose();
    bindingContext.dispose();
    observables.dispose();
    refreshIcon.dispose();
    super.dispose();
  }

  @Override
  public void setFont(Font font) {
    super.setFont(font);
    expandableComposite.setFont(font);
    FontUtil.convertFontToBold(expandableComposite);
  }
}
