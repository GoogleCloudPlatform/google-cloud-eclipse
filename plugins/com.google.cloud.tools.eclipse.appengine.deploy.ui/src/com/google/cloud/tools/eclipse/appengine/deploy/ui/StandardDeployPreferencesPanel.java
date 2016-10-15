/*******************************************************************************
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
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.ObservablesManager;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.osgi.service.prefs.BackingStoreException;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.appengine.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.appengine.login.ui.AccountSelectorObservableValue;
import com.google.cloud.tools.eclipse.ui.util.FontUtil;
import com.google.cloud.tools.eclipse.ui.util.databinding.BucketNameValidator;
import com.google.cloud.tools.eclipse.ui.util.databinding.ProjectIdInputValidator;
import com.google.cloud.tools.eclipse.ui.util.databinding.ProjectVersionValidator;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.ErrorHandler;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.QueryParameterProvider;
import com.google.common.base.Preconditions;

public class StandardDeployPreferencesPanel extends DeployPreferencesPanel {

  private static final String APPENGINE_VERSIONS_URL = "https://console.cloud.google.com/appengine/versions";
  private static final String URI_PARAM_PROJECT = "project";

  private static final int INDENT_CHECKBOX_ENABLED_WIDGET = 10;

  private static Logger logger = Logger.getLogger(DeployPropertyPage.class.getName());

  private AccountSelector accountSelector;

  private Label projectIdLabel;
  private Text projectId;

  private Button overrideDefaultVersionButton;
  private Label versionLabel;
  private Text version;

  private Button autoPromoteButton;

  private Button stopPreviousVersionButton;

  private Button overrideDefaultBucketButton;
  private Label bucketLabel;
  private Text bucket;

  private ExpandableComposite expandableComposite;

  private DeployPreferencesModel model;
  private ObservablesManager observables;
  private DataBindingContext bindingContext;

  private Runnable layoutChangedHandler;
  private boolean requireValues = true;

  public StandardDeployPreferencesPanel(Composite parent, IProject project,
      IGoogleLoginService loginService, Runnable layoutChangedHandler, boolean requireValues) {
    super(parent, SWT.NONE);

    this.layoutChangedHandler = layoutChangedHandler;
    this.requireValues = requireValues;

    createCredentialSection(loginService);

    createProjectIdSection();

    createProjectVersionSection();

    createPromoteSection();

    createAdvancedSection();

    Dialog.applyDialogFont(this);

    GridLayoutFactory.fillDefaults().spacing(0, 0).generateLayout(this);

    loadPreferences(project);

    setupDataBinding();
  }

  private void setupDataBinding() {
    bindingContext = new DataBindingContext();

    setupAccountEmailDataBinding(bindingContext);
    setupProjectIdDataBinding(bindingContext);
    setupProjectVersionDataBinding(bindingContext);
    setupAutoPromoteDataBinding(bindingContext);
    setupBucketDataBinding(bindingContext);

    observables = new ObservablesManager();
    observables.addObservablesFromContext(bindingContext, true, true);
  }

  private void setupAccountEmailDataBinding(DataBindingContext context) {
    final IObservableValue accountEmailModel = PojoProperties.value("accountEmail").observe(model);
    context.bindValue(new AccountSelectorObservableValue(accountSelector), accountEmailModel);

    if (requireValues) {
      context.addValidationStatusProvider(new FixedMultiValidator() {
        @Override
        protected IStatus validate() {
          String email = (String) accountEmailModel.getValue();
          // It's possible that no account is selected while a valid email has been saved in the
          // model (if the corresponding account is signed out), so check the actual selection too.
          if (email.isEmpty() || accountSelector.getSelectedEmail().isEmpty()) {
            return ValidationStatus.error(Messages.getString("error.account.missing"));
          }
          return ValidationStatus.ok();
        }
      });
    }
  }

  private void setupProjectIdDataBinding(DataBindingContext context) {
    ISWTObservableValue projectIdField = WidgetProperties.text(SWT.Modify).observe(projectId);

    IObservableValue projectIdModel = PojoProperties.value("projectId").observe(model);

    context.bindValue(projectIdField, projectIdModel,
                      new UpdateValueStrategy().setAfterGetValidator(new ProjectIdInputValidator(requireValues)),
                      new UpdateValueStrategy().setAfterGetValidator(new ProjectIdInputValidator(requireValues)));
  }

  private void setupProjectVersionDataBinding(DataBindingContext context) {
    ISWTObservableValue overrideButton = WidgetProperties.selection().observe(overrideDefaultVersionButton);
    ISWTObservableValue versionField = WidgetProperties.text(SWT.Modify).observe(version);
    ISWTObservableValue versionLabelEnablement = WidgetProperties.enabled().observe(versionLabel);
    ISWTObservableValue versionFieldEnablement = WidgetProperties.enabled().observe(version);

    // use an intermediary value to control the enabled state of the label and the field based on the override
    // checkbox's state
    WritableValue enablement = new WritableValue();
    context.bindValue(overrideButton, enablement);
    context.bindValue(versionLabelEnablement, enablement);
    context.bindValue(versionFieldEnablement, enablement);

    IObservableValue overrideModel = PojoProperties.value("overrideDefaultVersioning").observe(model);
    IObservableValue versionModel = PojoProperties.value("version").observe(model);

    context.bindValue(enablement, overrideModel);
    context.bindValue(versionField, versionModel);

    context.addValidationStatusProvider(new OverrideValidator(overrideButton,
                                                              versionField,
                                                              new ProjectVersionValidator()));
  }

  private void setupAutoPromoteDataBinding(DataBindingContext context) {
    ISWTObservableValue promoteButton = WidgetProperties.selection().observe(autoPromoteButton);
    ISWTObservableValue stopPreviousVersion = WidgetProperties.selection().observe(stopPreviousVersionButton);
    ISWTObservableValue stopPreviousVersionEnablement = WidgetProperties.enabled().observe(stopPreviousVersionButton);

    // use an intermediary value to control the enabled state of stopPreviousVersionButton based on the promote
    // checkbox's state
    WritableValue enablement = new WritableValue();
    context.bindValue(promoteButton, enablement);
    context.bindValue(stopPreviousVersionEnablement, enablement);

    IObservableValue promoteModel = PojoProperties.value("autoPromote").observe(model);
    IObservableValue stopPreviousVersionModel = PojoProperties.value("stopPreviousVersion").observe(model);

    context.bindValue(promoteButton, promoteModel);
    context.bindValue(stopPreviousVersion, stopPreviousVersionModel);
  }

  private void setupBucketDataBinding(DataBindingContext context) {
    ISWTObservableValue overrideButton = WidgetProperties.selection().observe(overrideDefaultBucketButton);
    ISWTObservableValue bucketField = WidgetProperties.text(SWT.Modify).observe(bucket);
    ISWTObservableValue bucketLabelEnablement = WidgetProperties.enabled().observe(bucketLabel);
    ISWTObservableValue bucketFieldEnablement = WidgetProperties.enabled().observe(bucket);

    // use an intermediary value to control the enabled state of the label and the field based on the override
    // checkbox's state
    WritableValue enablement = new WritableValue();
    context.bindValue(overrideButton, enablement);
    context.bindValue(bucketLabelEnablement, enablement);
    context.bindValue(bucketFieldEnablement, enablement);

    IObservableValue overrideModelObservable = PojoProperties.value("overrideDefaultBucket").observe(model);
    IObservableValue bucketModelObservable = PojoProperties.value("bucket").observe(model);

    context.bindValue(enablement, overrideModelObservable);
    context.bindValue(bucketField, bucketModelObservable);

    context.addValidationStatusProvider(new OverrideValidator(overrideButton,
                                                              bucketField,
                                                              new BucketNameValidator()));
  }

  @Override
  public boolean savePreferences() {
    try {
      model.savePreferences();
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

  private void loadPreferences(IProject project) {
    model = new DeployPreferencesModel(project);
  }

  public Credential getSelectedCredential() {
    return accountSelector.getSelectedCredential();
  }

  private void createCredentialSection(IGoogleLoginService loginService) {
    Composite accountComposite = new Composite(this, SWT.NONE);

    new Label(accountComposite, SWT.LEFT).setText(
        Messages.getString("deploy.preferences.dialog.label.selectAccount"));

    accountSelector = new AccountSelector(accountComposite, loginService,
        Messages.getString("deploy.preferences.dialog.accountSelector.login"));
    GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(accountComposite);
  }

  private void createProjectIdSection() {
    Composite projectIdComposite = new Composite(this, SWT.NONE);

    projectIdLabel = new Label(projectIdComposite, SWT.LEFT);
    projectIdLabel.setText(Messages.getString("project.id"));

    projectId = new Text(projectIdComposite, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(projectIdComposite);
  }

  private void createProjectVersionSection() {
    Composite versionComposite = new Composite(this, SWT.NONE);

    overrideDefaultVersionButton = new Button(versionComposite, SWT.CHECK);
    overrideDefaultVersionButton.setText(Messages.getString("use.custom.versioning"));
    overrideDefaultVersionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

    versionLabel = new Label(versionComposite, SWT.NONE);
    versionLabel.setText(Messages.getString("project.version"));
    GridData layoutData = GridDataFactory.swtDefaults().create();
    layoutData.horizontalIndent = INDENT_CHECKBOX_ENABLED_WIDGET;
    versionLabel.setLayoutData(layoutData);

    version = new Text(versionComposite, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(versionComposite);
  }

  private void createPromoteSection() {
    Composite promoteComposite = new Composite(this, SWT.NONE);
    autoPromoteButton = new Button(promoteComposite, SWT.CHECK);
    autoPromoteButton.setText(Messages.getString("auto.promote"));

    Link manualPromoteLink = new Link(promoteComposite, SWT.NONE);
    GridData layoutData = GridDataFactory.swtDefaults().create();
    layoutData.horizontalIndent = INDENT_CHECKBOX_ENABLED_WIDGET;
    manualPromoteLink.setLayoutData(layoutData);
    manualPromoteLink.setText(Messages.getString("deploy.manual.link", APPENGINE_VERSIONS_URL));
    manualPromoteLink.setFont(promoteComposite.getFont());
    manualPromoteLink.addSelectionListener(new OpenUriSelectionListener(new QueryParameterProvider() {
      @Override
      public Map<String, String> getParameters() {
        return Collections.singletonMap(URI_PARAM_PROJECT, projectId.getText().trim());
      }
    }, new ErrorHandler() {
      @Override
      public void handle(Exception ex) {
        MessageDialog.openError(getShell(), Messages.getString("cannot.open.browser"), ex.getLocalizedMessage());
      }
    }));

    stopPreviousVersionButton = new Button(promoteComposite, SWT.CHECK);
    stopPreviousVersionButton.setText(Messages.getString("stop.previous.version"));
    GridDataFactory.swtDefaults().indent(INDENT_CHECKBOX_ENABLED_WIDGET, 0).applyTo(stopPreviousVersionButton);

    GridLayoutFactory.fillDefaults().generateLayout(promoteComposite);
  }

  private void createAdvancedSection() {
    createExpandableComposite();
    final Composite bucketComposite = createBucketSection(expandableComposite);
    expandableComposite.setClient(bucketComposite);
    expandableComposite.addExpansionListener(new ExpansionAdapter() {
      @Override
      public void expansionStateChanged(ExpansionEvent e) {
        handleExpansionStateChanged();
      }
    });
    GridLayoutFactory.fillDefaults().generateLayout(expandableComposite);
  }

  private void createExpandableComposite() {
    expandableComposite = new ExpandableComposite(this, SWT.NONE, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
    expandableComposite.setText(Messages.getString("settings.advanced"));
    expandableComposite.setExpanded(false);
    FontUtil.convertFontToBold(expandableComposite);
    GridDataFactory.fillDefaults().applyTo(expandableComposite);
    getFormToolkit().adapt(expandableComposite, true, true);
  }

  private Composite createBucketSection(Composite parent) {
    Composite bucketComposite = new Composite(parent, SWT.NONE);

    overrideDefaultBucketButton = new Button(bucketComposite, SWT.CHECK);
    overrideDefaultBucketButton.setText(Messages.getString("use.custom.bucket"));
    overrideDefaultBucketButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

    bucketLabel = new Label(bucketComposite, SWT.RADIO);
    bucketLabel.setText(Messages.getString("bucket.name"));

    bucket = new Text(bucketComposite, SWT.LEFT | SWT.SINGLE | SWT.BORDER);

    GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(bucketComposite);
    return bucketComposite;
  }

  /**
   * Validates a checkbox and text field as follows:
   * <ol>
   * <li>if the checkbox is unselected -> valid
   * <li>if the checkbox is selected -> the result is determined by the provided <code>validator</code> used
   * on the value of the text field
   * </ol>
   *
   */
  private static class OverrideValidator extends FixedMultiValidator {

    private ISWTObservableValue selectionObservable;
    private ISWTObservableValue textObservable;
    private IValidator validator;

    /**
     * @param selection must be an observable for a checkbox, i.e. a {@link Button} with {@link SWT#CHECK} style
     * @param text must be an observable for a {@link Text}
     * @param validator must be a validator for String values, will be applied to <code>text.getValue()</code>
     */
    public OverrideValidator(ISWTObservableValue selection, ISWTObservableValue text, IValidator validator) {
      Preconditions.checkArgument(text.getWidget() instanceof Text,
                                  "text is an observable for {0}, should be for {1}",
                                  text.getWidget().getClass().getName(),
                                  Text.class.getName());
      Preconditions.checkArgument(selection.getWidget() instanceof Button,
                                  "selection is an observable for {0}, should be for {1}",
                                  selection.getWidget().getClass().getName(),
                                  Button.class.getName());
      Preconditions.checkArgument((selection.getWidget().getStyle() & SWT.CHECK) != 0,
          "selection must be an observable for a checkbox");
      this.selectionObservable = selection;
      this.textObservable = text;
      this.validator = validator;
    }

    @Override
    protected IStatus validate() {
      if (Boolean.FALSE.equals(selectionObservable.getValue())) {
        return ValidationStatus.ok();
      }
      return validator.validate(textObservable.getValue());
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
  };

  @Override
  public DataBindingContext getDataBindingContext() {
    return bindingContext;
  }

  @Override
  public void resetToDefaults() {
    model.resetToDefaults();
    bindingContext.updateTargets();
  }

  @Override
  public void dispose() {
    if (bindingContext != null) {
      bindingContext.dispose();
    }
    if (observables != null) {
      observables.dispose();
    }
    super.dispose();
  }

  private void handleExpansionStateChanged() {
    if (layoutChangedHandler != null) {
      layoutChangedHandler.run();
    }
  }

  @Override
  public void setFont(Font font) {
    super.setFont(font);
    expandableComposite.setFont(font);
    FontUtil.convertFontToBold(expandableComposite);
  }
}
