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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.osgi.service.prefs.BackingStoreException;

import com.google.cloud.tools.eclipse.ui.util.FontUtil;
import com.google.cloud.tools.eclipse.ui.util.databinding.BucketNameValidator;
import com.google.cloud.tools.eclipse.ui.util.databinding.ProjectIdValidator;
import com.google.cloud.tools.eclipse.ui.util.databinding.ProjectVersionValidator;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUrlSelectionListener;
import com.google.common.base.Preconditions;

public class DeployPreferencesPanel extends Composite {

  private static final String APPENGINE_DASHBOARD_URL = "https://console.cloud.google.com/appengine";

  private static final int INDENT_CHECKBOX_ENABLED_WIDGET = 10;

  private static Logger logger = Logger.getLogger(DeployPropertyPage.class.getName());

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

  private DeployPreferencesModel model;
  private ObservablesManager observables;
  private DataBindingContext bindingContext;

  public DeployPreferencesPanel(Composite parent, IProject project) {
    super(parent, SWT.NONE);
    GridLayout layout = new GridLayout(1, false);
    // set margin to 0 to meet expectations of super.createContents(Composite)
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    setLayout(layout);

    createProjectIdSection();

    createProjectVersionSection();

    createPromoteSection();

    createStopPreviousVersionSection();

    createAdvancedSection();

    Dialog.applyDialogFont(this);

    loadPreferences(project);

    setupDataBinding();
  }

  private void setupDataBinding() {
    bindingContext = new DataBindingContext();

    setupProjectIdDataBinding(bindingContext);
    setupProjectVersionDataBinding(bindingContext);
    setupAutoPromoteDataBinding(bindingContext);
    setupStopPreviousVersionDataBinding(bindingContext);
    setupBucketDataBinding(bindingContext);

    observables = new ObservablesManager();
    observables.addObservablesFromContext(bindingContext, true, true);
  }

  private void setupProjectIdDataBinding(DataBindingContext context) {
    ISWTObservableValue projectIdField = WidgetProperties.text(SWT.Modify).observe(projectId);

    IObservableValue projectIdModel = PojoProperties.value("projectId").observe(model);

    context.bindValue(projectIdField, projectIdModel,
                      new UpdateValueStrategy().setAfterGetValidator(new ProjectIdValidator()),
                      new UpdateValueStrategy().setAfterGetValidator(new ProjectIdValidator()));
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
    IObservableValue promoteModel = PojoProperties.value("autoPromote").observe(model);
    context.bindValue(promoteButton, promoteModel);
  }

  private void setupStopPreviousVersionDataBinding(DataBindingContext context) {
    ISWTObservableValue stopPreviousVersion = WidgetProperties.selection().observe(stopPreviousVersionButton);
    IObservableValue stopPreviousVersionModel = PojoProperties.value("stopPreviousVersion").observe(model);
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

  private void createProjectIdSection() {
    Composite projectIdComp = new Composite(this, SWT.NONE);
    projectIdComp.setLayout(new GridLayout(2, false));
    projectIdComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    projectIdLabel = new Label(projectIdComp, SWT.LEFT);
    projectIdLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    projectIdLabel.setText(Messages.getString("project.id"));

    projectId = new Text(projectIdComp, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    projectId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  private void createProjectVersionSection() {
    Composite versionComp = new Composite(this, SWT.NONE);
    versionComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    versionComp.setLayout(new GridLayout(2, false));

    overrideDefaultVersionButton = new Button(versionComp, SWT.CHECK);
    overrideDefaultVersionButton.setText(Messages.getString("use.custom.versioning"));
    overrideDefaultVersionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

    versionLabel = new Label(versionComp, SWT.NONE);
    versionLabel.setText(Messages.getString("project.version"));
    GridData layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    layoutData.horizontalIndent = INDENT_CHECKBOX_ENABLED_WIDGET;
    versionLabel.setLayoutData(layoutData);

    version = new Text(versionComp, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    version.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  private void createPromoteSection() {
    Composite promoteComp = new Composite(this, SWT.NONE);
    promoteComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    promoteComp.setLayout(new GridLayout(1, false));
    autoPromoteButton = new Button(promoteComp, SWT.CHECK);
    autoPromoteButton.setText(Messages.getString("auto.promote"));
    autoPromoteButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

    Link manualPromoteLink = new Link(promoteComp, SWT.NONE);
    GridData layoutData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    layoutData.horizontalIndent = INDENT_CHECKBOX_ENABLED_WIDGET;
    manualPromoteLink.setLayoutData(layoutData);
    manualPromoteLink.setText(Messages.getString("deploy.manual.link", APPENGINE_DASHBOARD_URL));
    manualPromoteLink.setFont(promoteComp.getFont());
    manualPromoteLink.addSelectionListener(new OpenUrlSelectionListener(new OpenUrlSelectionListener.ErrorHandler() {
      @Override
      public void handle(Exception ex) {
        MessageDialog.openError(getShell(), Messages.getString("cannot.open.browser"), ex.getLocalizedMessage());
      }
    }));
  }

  private void createStopPreviousVersionSection() {
    Composite composite = new Composite(this, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    composite.setLayout(new GridLayout(1, false));

    stopPreviousVersionButton = new Button(composite, SWT.CHECK);
    stopPreviousVersionButton.setText(Messages.getString("stop.previous.version"));
    stopPreviousVersionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
  }

  private void createAdvancedSection() {
    final ExpandableComposite expandableComposite = createExpandableComposite();
    final Composite defaultBucketComp = createBucketSection(expandableComposite);
    expandableComposite.setClient(defaultBucketComp);
    expandableComposite.addExpansionListener(new ExpansionAdapter() {
      @Override
      public void expansionStateChanged(ExpansionEvent e) {
        updateLayout(expandableComposite);
      }
  });
  }

  /**
   * Update the ancestral ScrolledComposite that there's been a change.
   */
  protected void updateLayout(ExpandableComposite expandableComposite) {
    Control parent = expandableComposite.getParent();
    while (parent != null) {
      if (parent instanceof ScrolledComposite) {
        ScrolledComposite sc = (ScrolledComposite) parent;
        sc.setMinSize(sc.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
      }
      parent = parent.getParent();
    }
  }

  private ExpandableComposite createExpandableComposite() {
    ExpandableComposite expandableComposite =
        new ExpandableComposite(this, SWT.NONE, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
    expandableComposite.setText(Messages.getString("settings.advanced"));
    expandableComposite.setExpanded(false);
    expandableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    FontUtil.convertFontToBold(expandableComposite);
    return expandableComposite;
  }

  private Composite createBucketSection(Composite parent) {
    Composite defaultBucketComp = new Composite(parent, SWT.NONE);
    defaultBucketComp.setLayout(new GridLayout(1, true));

    overrideDefaultBucketButton = new Button(defaultBucketComp, SWT.CHECK);
    overrideDefaultBucketButton.setText(Messages.getString("use.custom.bucket"));
    overrideDefaultBucketButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

    Composite customBucketComp = new Composite(defaultBucketComp, SWT.NONE);
    customBucketComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    customBucketComp.setLayout(new GridLayout(2, false));

    bucketLabel = new Label(customBucketComp, SWT.RADIO);
    bucketLabel.setText(Messages.getString("bucket.name"));
    bucketLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    bucket = new Text(customBucketComp, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    bucket.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    return defaultBucketComp;
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
  private static class OverrideValidator extends MultiValidator {

    private ISWTObservableValue selectionObservable;
    private ISWTObservableValue textObservable;
    private IValidator validator;

    /**
     * @param selection must be an observable for a checkbox, i.e. a {@link Button} with {@link SWT#CHECK} style
     * @param text must be an observable for a {@link Text}
     * @param validator must be a validator for String values, will be applied to <code>text.getValue()</code>
     */
    public OverrideValidator(ISWTObservableValue selection, ISWTObservableValue text, IValidator validator) {
      super(selection.getRealm());
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

    @Override
    public IObservableList getTargets() {
      /**
       * BUGFIX: https://bugs.eclipse.org/bugs/show_bug.cgi?id=312785
       */
      if( isDisposed() ) {
        return Observables.emptyObservableList();
      }
      return super.getTargets();
    }
  }

  public DataBindingContext getDataBindingContext() {
    return bindingContext;
  }

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
}
