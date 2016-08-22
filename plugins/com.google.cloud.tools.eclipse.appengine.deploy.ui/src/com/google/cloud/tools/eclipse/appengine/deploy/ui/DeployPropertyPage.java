
package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.ObservablesManager;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.preference.PreferencePageSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.google.cloud.tools.eclipse.ui.util.FontUtil;
import com.google.cloud.tools.eclipse.ui.util.databinding.BucketNameValidator;
import com.google.cloud.tools.eclipse.ui.util.databinding.ProjectIdValidator;
import com.google.cloud.tools.eclipse.ui.util.databinding.ProjectVersionValidator;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUrlSelectionListener;
import com.google.cloud.tools.eclipse.util.AdapterUtil;

public class DeployPropertyPage extends PropertyPage {

  private static final String APPENGINE_DASHBOARD_URL = "https://console.cloud.google.com/appengine";

  private static final int INDENT_CHECKBOX_ENABLED_WIDGET = 10;

  private static final String PREFERENCE_STORE_QUALIFIER = "com.google.cloud.tools.eclipse.appengine.deploy";
  private static final String PREF_PROMPT_FOR_PROJECT_ID = "project.id.prompt"; // boolean
  private static final String PREF_PROJECT_ID = "project.id";
  private static final String PREF_OVERRIDE_DEFAULT_VERSIONING = "project.version.default"; // boolean
  private static final String PREF_CUSTOM_VERSION = "project.version";
  private static final String PREF_ENABLE_AUTO_PROMOTE = "project.promote"; // boolean
  private static final String PREF_OVERRIDE_DEFAULT_BUCKET = "project.bucket.default"; // boolean
  private static final String PREF_CUSTOM_BUCKET = "project.bucket";

  private static Logger logger = Logger.getLogger(DeployPropertyPage.class.getName());

  private Label projectIdLabel;
  private Text projectId;
  private Button promptForProjectIdButton;

  private Button overrideDefaultVersionButton;
  private Label versionLabel;
  private Text version;

  private Button autoPromoteButton;

  private Button overrideDefaultBucketButton;
  private Label bucketLabel;
  private Text bucket;

  private Model model = new Model();
  private ObservablesManager observables;
  private DataBindingContext bindingContext;

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    noDefaultButton();

    createProjectIdSection(container);

    createProjectVersionSection(container);

    createPromoteSection(container);

    createAdvancedSection(container);

    Dialog.applyDialogFont(container);

    loadPreferences();

    setupDataBinding();

    return container;
  }

  private void setupDataBinding() {
    bindingContext = new DataBindingContext();

    setupProjectIdDataBinding(bindingContext);
    setupProjectVersionDataBinding(bindingContext);
    setupAutoPromoteDataBinding(bindingContext);
    setupBucketDataBinding(bindingContext);

    PreferencePageSupport.create(this, bindingContext);
    bindingContext.updateTargets();
    observables = new ObservablesManager();
    observables.addObservablesFromContext(bindingContext, true, true);
  }

  private void setupProjectIdDataBinding(DataBindingContext context) {
    context.bindValue(WidgetProperties.selection().observe(promptForProjectIdButton),
                      model.promptForProjectId);
    ProjectIdValidator projectIdValidator = new ProjectIdValidator();
    context.bindValue(WidgetProperties.text(SWT.Modify).observe(projectId),
                      model.projectId,
                      new UpdateValueStrategy().setAfterGetValidator(projectIdValidator),
                      new UpdateValueStrategy().setAfterGetValidator(projectIdValidator));
  }

  private void setupProjectVersionDataBinding(DataBindingContext context) {
    final ISWTObservableValue overrideObservable = WidgetProperties.selection().observe(overrideDefaultVersionButton);
    final ISWTObservableValue versionObservable = WidgetProperties.text(SWT.Modify).observe(version);

    context.bindValue(overrideObservable, model.overrideDefaultVersioning);
    context.bindValue(versionObservable, model.version);
    context.bindValue(WidgetProperties.enabled().observe(versionLabel), model.overrideDefaultVersioning);
    context.bindValue(WidgetProperties.enabled().observe(version), model.overrideDefaultVersioning);

    context.addValidationStatusProvider(new OverrideValidator(overrideObservable,
                                                              overrideObservable,
                                                              new ProjectVersionValidator()));

  }

  private void setupAutoPromoteDataBinding(DataBindingContext context) {
    context.bindValue(WidgetProperties.selection().observe(autoPromoteButton), model.autoPromote);
  }

  private void setupBucketDataBinding(DataBindingContext context) {
    final ISWTObservableValue overrideObservable = WidgetProperties.selection().observe(overrideDefaultBucketButton);
    final ISWTObservableValue bucketObservable = WidgetProperties.text(SWT.Modify).observe(bucket);

    context.bindValue(overrideObservable, model.overrideDefaultBucket);
    context.bindValue(bucketObservable, model.bucket);
    context.bindValue(WidgetProperties.enabled().observe(bucketLabel), model.overrideDefaultBucket);
    context.bindValue(WidgetProperties.enabled().observe(bucket), model.overrideDefaultBucket);

    context.addValidationStatusProvider(new OverrideValidator(overrideObservable,
                                                              bucketObservable,
                                                              new BucketNameValidator()));
  }

  @Override
  public boolean performOk() {
    try {
      if (isValid()) {
        savePreferences();
        return true;
      } else {
        return false;
      }
    } catch (IOException exception) {
      logger.log(Level.SEVERE, "Could not save deploy preferences", exception);
      return false;
    }
  }

  private void savePreferences() throws IOException {
    IPreferenceStore preferenceStore = getPreferenceStore();

    preferenceStore.setValue(PREF_PROJECT_ID, model.getProjectId());
    preferenceStore.setValue(PREF_PROMPT_FOR_PROJECT_ID, model.isPromptForProjectId());
    preferenceStore.setValue(PREF_OVERRIDE_DEFAULT_VERSIONING, model.isOverrideDefaultVersioning());
    preferenceStore.setValue(PREF_CUSTOM_VERSION, model.getVersion());
    preferenceStore.setValue(PREF_ENABLE_AUTO_PROMOTE, model.isAutoPromote());
    preferenceStore.setValue(PREF_OVERRIDE_DEFAULT_BUCKET, model.isOverrideDefaultBucket());
    preferenceStore.setValue(PREF_CUSTOM_BUCKET, model.getBucket());

    ScopedPreferenceStore scopedPreferenceStore = (ScopedPreferenceStore) preferenceStore;
    if (scopedPreferenceStore.needsSaving()) {
      scopedPreferenceStore.save();
    }
  }

  private void loadPreferences() {
    IPreferenceStore preferenceStore = getPreferenceStore();

    model.setProjectId(preferenceStore.getString(PREF_PROJECT_ID));
    model.setPromptForProjectId(preferenceStore.getBoolean(PREF_PROMPT_FOR_PROJECT_ID));
    model.setOverrideDefaultVersioning(preferenceStore.getBoolean(PREF_OVERRIDE_DEFAULT_VERSIONING));
    model.setVersion(preferenceStore.getString(PREF_CUSTOM_VERSION));
    model.setAutoPromote(preferenceStore.getBoolean(PREF_ENABLE_AUTO_PROMOTE));
    model.setOverrideDefaultBucket(preferenceStore.getBoolean(PREF_OVERRIDE_DEFAULT_BUCKET));
    model.setBucket(preferenceStore.getString(PREF_CUSTOM_BUCKET));
  }

  @Override
  protected IPreferenceStore doGetPreferenceStore() {
    IProject project = AdapterUtil.adapt(getElement(), IProject.class);
    return new ScopedPreferenceStore(new ProjectScope(project), PREFERENCE_STORE_QUALIFIER);
  }

  private void createProjectIdSection(Composite parent) {
    Composite projectIdComp = new Composite(parent, SWT.NONE);
    projectIdComp.setLayout(new GridLayout(2, false));
    projectIdComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    projectIdLabel = new Label(projectIdComp, SWT.LEFT);
    projectIdLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    projectIdLabel.setText(Messages.getString("project.id"));

    projectId = new Text(projectIdComp, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    projectId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    promptForProjectIdButton = new Button(projectIdComp, SWT.CHECK);
    promptForProjectIdButton.setText(Messages.getString("deploy.override.projectid"));
    GridData layoutData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    layoutData.horizontalIndent = INDENT_CHECKBOX_ENABLED_WIDGET;
    promptForProjectIdButton.setLayoutData(layoutData);
  }

  private void createProjectVersionSection(Composite parent) {
    Composite versionComp = new Composite(parent, SWT.NONE);
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

  private void createPromoteSection(Composite parent) {
    Composite promoteComp = new Composite(parent, SWT.NONE);
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
        setMessage(Messages.getString("cannot.open.browser", ex.getLocalizedMessage()),
                   IMessageProvider.WARNING);
      }
    }));
  }

  private void createAdvancedSection(Composite parent) {
    ExpandableComposite expandableComposite = createExpandableComposite(parent);
    Composite defaultBucketComp = createBucketSection(expandableComposite);
    expandableComposite.setClient(defaultBucketComp);
  }

  private ExpandableComposite createExpandableComposite(Composite parent) {
    ExpandableComposite expandableComposite = new ExpandableComposite(parent, SWT.NONE,
                                                                      ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
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

  private static class Model {

    private WritableValue promptForProjectId = new WritableValue(Boolean.TRUE, Boolean.class);
    private WritableValue projectId = new WritableValue();
    private WritableValue overrideDefaultVersioning = new WritableValue(Boolean.FALSE, Boolean.class);
    private WritableValue version = new WritableValue();
    private WritableValue autoPromote = new WritableValue(Boolean.TRUE, Boolean.class);
    private WritableValue overrideDefaultBucket = new WritableValue(Boolean.FALSE, Boolean.class);
    private WritableValue bucket = new WritableValue();

    public boolean isPromptForProjectId() {
      return (boolean) promptForProjectId.getValue();
    }

    public void setPromptForProjectId(boolean promptForProjectId) {
      this.promptForProjectId.setValue(promptForProjectId);
    }

    public String getProjectId() {
      return (String) projectId.getValue();
    }

    public void setProjectId(String projectId) {
      this.projectId.setValue(projectId);
    }

    public boolean isOverrideDefaultVersioning() {
      return (boolean) overrideDefaultVersioning.getValue();
    }

    public void setOverrideDefaultVersioning(boolean overrideDefaultVersioning) {
      this.overrideDefaultVersioning.setValue(overrideDefaultVersioning);
    }

    public String getVersion() {
      return (String) version.getValue();
    }

    public void setVersion(String version) {
      this.version.setValue(version);
    }

    public boolean isAutoPromote() {
      return (boolean) autoPromote.getValue();
    }

    public void setAutoPromote(boolean autoPromote) {
      this.autoPromote.setValue(autoPromote);
    }

    public boolean isOverrideDefaultBucket() {
      return (boolean) overrideDefaultBucket.getValue();
    }

    public void setOverrideDefaultBucket(boolean overrideDefaultBucket) {
      this.overrideDefaultBucket.setValue(overrideDefaultBucket);
    }

    public String getBucket() {
      return (String) bucket.getValue();
    }

    public void setBucket(String bucket) {
      this.bucket.setValue(bucket);
    }
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

  /**
   * Validates a checkbox and text field as follows:
   * <ol>
   * <li>if the checkbox is unselected -> valid
   * <li>if the checkbox is selected -> the result is determined by the provided <code>validator</code> used
   * on the value of the text field
   * </ol>
   *
   * <i>The class does not enforce the {@link ISWTObservableValue}s passed in to the constructor to be of a
   * checkbox and a text field</i>
   */
  private static class OverrideValidator extends MultiValidator {

    private ISWTObservableValue selectionObservable;
    private ISWTObservableValue textObservable;
    private IValidator validator;

    public OverrideValidator(ISWTObservableValue selection, ISWTObservableValue text, IValidator validator) {
      super(selection.getRealm());
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
}
