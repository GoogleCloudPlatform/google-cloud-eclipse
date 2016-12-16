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
import com.google.cloud.tools.eclipse.appengine.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.appengine.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.appengine.login.ui.AccountSelectorObservableValue;
import com.google.cloud.tools.eclipse.ui.util.FontUtil;
import com.google.cloud.tools.eclipse.ui.util.databinding.BucketNameValidator;
import com.google.cloud.tools.eclipse.ui.util.databinding.ProjectIdInputValidator;
import com.google.cloud.tools.eclipse.ui.util.databinding.ProjectVersionValidator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.osgi.service.prefs.BackingStoreException;

public class StandardDeployPreferencesPanel extends DeployPreferencesPanel {

  private static final String APPENGINE_VERSIONS_URL =
      "https://console.cloud.google.com/appengine/versions";

  private static final Logger logger = Logger.getLogger(
      StandardDeployPreferencesPanel.class.getName());

  private AccountSelector accountSelector;

  private Label projectIdLabel;
  private Text projectId;

  private Button overrideDefaultVersionButton;
  private Text version;

  private Button autoPromoteButton;

  private Button stopPreviousVersionButton;

  private Button overrideDefaultBucketButton;
  private Text bucket;

  private ExpandableComposite expandableComposite;

  @VisibleForTesting
  DeployPreferencesModel model;
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

    GridLayoutFactory.fillDefaults().generateLayout(this);

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
    AccountSelectorObservableValue accountSelectorObservableValue =
        new AccountSelectorObservableValue(accountSelector);
    UpdateValueStrategy modelToTarget =
        new UpdateValueStrategy().setConverter(new Converter(String.class, String.class) {
          @Override
          public Object convert(Object expectedEmail) {
            // Expected to be an email address, but must also ensure is a currently logged-in
            // account
            if (expectedEmail instanceof String
                && accountSelector.isEmailAvailable((String) expectedEmail)) {
              return expectedEmail;
            } else {
              return null;
            }
          }
        });

    final IObservableValue accountEmailModel = PojoProperties.value("accountEmail").observe(model);

    Binding binding = context.bindValue(accountSelectorObservableValue, accountEmailModel,
        new UpdateValueStrategy(), modelToTarget);
    /*
     * Trigger an explicit target -> model update for the auto-select-single-account case. When the
     * model has a null account but there is exactly 1 login account, then the AccountSelector
     * automatically selects that account. That change means the AccountSelector is at odds with the
     * model.
     */
    binding.updateTargetToModel();
    context.addValidationStatusProvider(new AccountSelectorValidator(requireValues, accountSelector,
        accountSelectorObservableValue));
  }

  private void setupProjectIdDataBinding(DataBindingContext context) {
    ISWTObservableValue projectIdField = WidgetProperties.text(SWT.Modify).observe(projectId);

    IObservableValue projectIdModel = PojoProperties.value("projectId").observe(model);

    context.bindValue(projectIdField, projectIdModel,
        new UpdateValueStrategy().setAfterGetValidator(new ProjectIdInputValidator(requireValues)),
        new UpdateValueStrategy().setAfterGetValidator(new ProjectIdInputValidator(requireValues)));
  }

  private void setupProjectVersionDataBinding(DataBindingContext context) {
    ISWTObservableValue overrideButton =
        WidgetProperties.selection().observe(overrideDefaultVersionButton);
    ISWTObservableValue versionField = WidgetProperties.text(SWT.Modify).observe(version);
    ISWTObservableValue versionFieldEnablement = WidgetProperties.enabled().observe(version);

    // use an intermediary value to control the enabled state of the the field based on the override
    // checkbox's state
    WritableValue enablement = new WritableValue();
    context.bindValue(overrideButton, enablement);
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
    ISWTObservableValue stopPreviousVersion =
        WidgetProperties.selection().observe(stopPreviousVersionButton);
    ISWTObservableValue stopPreviousVersionEnablement =
        WidgetProperties.enabled().observe(stopPreviousVersionButton);

    // use an intermediary value to control the enabled state of stopPreviousVersionButton
    // based on the promote checkbox's state
    WritableValue enablement = new WritableValue();
    context.bindValue(promoteButton, enablement);
    context.bindValue(stopPreviousVersionEnablement, enablement);

    IObservableValue promoteModel = PojoProperties.value("autoPromote").observe(model);
    IObservableValue stopPreviousVersionModel = PojoProperties.value("stopPreviousVersion").observe(model);

    context.bindValue(promoteButton, promoteModel);
    context.bindValue(stopPreviousVersion, stopPreviousVersionModel);
  }

  private void setupBucketDataBinding(DataBindingContext context) {
    ISWTObservableValue overrideButton =
        WidgetProperties.selection().observe(overrideDefaultBucketButton);
    ISWTObservableValue bucketField = WidgetProperties.text(SWT.Modify).observe(bucket);
    ISWTObservableValue bucketFieldEnablement = WidgetProperties.enabled().observe(bucket);

    // use an intermediary value to control the enabled state of the label and the field
    // based on the override checkbox's state
    WritableValue enablement = new WritableValue();
    context.bindValue(overrideButton, enablement);
    context.bindValue(bucketFieldEnablement, enablement);

    IObservableValue overrideModelObservable =
        PojoProperties.value("overrideDefaultBucket").observe(model);
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

    Label accountLabel = new Label(accountComposite, SWT.LEAD);
    accountLabel.setText(Messages.getString("deploy.preferences.dialog.label.selectAccount"));
    accountLabel.setToolTipText(Messages.getString("tooltip.account"));

    // If we don't require values, then don't auto-select accounts
    accountSelector = new AccountSelector(accountComposite, loginService,
        Messages.getString("deploy.preferences.dialog.accountSelector.login"), requireValues);
    accountSelector.setToolTipText(Messages.getString("tooltip.account"));
    GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(accountComposite);
  }

  private void createProjectIdSection() {
    Composite projectIdComposite = new Composite(this, SWT.NONE);

    projectIdLabel = new Label(projectIdComposite, SWT.LEAD);
    projectIdLabel.setText(Messages.getString("project.id"));
    projectIdLabel.setToolTipText(Messages.getString("tooltip.project.id"));
    GridData layoutData = GridDataFactory.swtDefaults().create();
    projectIdLabel.setLayoutData(layoutData);

    projectId = new Text(projectIdComposite, SWT.LEAD | SWT.SINGLE | SWT.BORDER);
    projectId.setToolTipText(Messages.getString("tooltip.project.id"));
    GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(projectIdComposite);
  }

  private void createProjectVersionSection() {
    Composite versionComposite = new Composite(this, SWT.NONE);

    overrideDefaultVersionButton = new Button(versionComposite, SWT.CHECK);
    overrideDefaultVersionButton.setText(Messages.getString("use.custom.versioning"));
    overrideDefaultVersionButton.setToolTipText(Messages.getString("tooltip.version"));
    GridData layoutData = GridDataFactory.swtDefaults().create();
    overrideDefaultVersionButton.setLayoutData(layoutData);

    version = new Text(versionComposite, SWT.LEAD | SWT.SINGLE | SWT.BORDER);
    version.setToolTipText(Messages.getString("tooltip.version"));
    GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(versionComposite);
  }

  private void createPromoteSection() {
    Composite promoteComposite = new Composite(this, SWT.NONE);
    autoPromoteButton = new Button(promoteComposite, SWT.CHECK);
    autoPromoteButton.setText(Messages.getString("auto.promote"));
    String manualPromoteMessage = Messages.getString(
        "tooltip.manual.promote.link", APPENGINE_VERSIONS_URL);
    autoPromoteButton.setToolTipText(manualPromoteMessage);

    stopPreviousVersionButton = new Button(promoteComposite, SWT.CHECK);
    stopPreviousVersionButton.setText(Messages.getString("stop.previous.version"));
    stopPreviousVersionButton.setToolTipText(Messages.getString("tooltip.stop.previous.version"));

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
    expandableComposite = new ExpandableComposite(this, SWT.NONE, ExpandableComposite.TWISTIE);
    FontUtil.convertFontToBold(expandableComposite);
    expandableComposite.setText(Messages.getString("settings.advanced"));
    expandableComposite.setExpanded(false);
    GridDataFactory.fillDefaults().applyTo(expandableComposite);
    getFormToolkit().adapt(expandableComposite, true, true);
  }

  private Composite createBucketSection(Composite parent) {
    Composite bucketComposite = new Composite(parent, SWT.NONE);

    overrideDefaultBucketButton = new Button(bucketComposite, SWT.CHECK);
    overrideDefaultBucketButton.setText(Messages.getString("use.custom.bucket"));
    GridData layoutData = GridDataFactory.swtDefaults().create();
    overrideDefaultBucketButton.setLayoutData(layoutData);
    overrideDefaultBucketButton.setToolTipText(Messages.getString("tooltip.staging.bucket"));

    bucket = new Text(bucketComposite, SWT.LEAD | SWT.SINGLE | SWT.BORDER);
    bucket.setToolTipText(Messages.getString("tooltip.staging.bucket"));

    GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(bucketComposite);
    return bucketComposite;
  }

  /**
   * Validates a checkbox and text field as follows:
   * <ol>
   * <li>if the checkbox is unselected -> valid
   * <li>if the checkbox is selected -> the result is determined by the provided
   * <code>validator</code> used on the value of the text field
   * </ol>
   */
  private static class OverrideValidator extends FixedMultiValidator {

    private ISWTObservableValue selectionObservable;
    private ISWTObservableValue textObservable;
    private IValidator validator;

    /**
     * @param selection must be an observable for a checkbox, i.e. a {@link Button} with
     *        {@link SWT#CHECK} style
     * @param text must be an observable for a {@link Text}
     * @param validator must be a validator for String values, will be applied to
     *        <code>text.getValue()</code>
     */
    public OverrideValidator(ISWTObservableValue selection, ISWTObservableValue text,
        IValidator validator) {
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
