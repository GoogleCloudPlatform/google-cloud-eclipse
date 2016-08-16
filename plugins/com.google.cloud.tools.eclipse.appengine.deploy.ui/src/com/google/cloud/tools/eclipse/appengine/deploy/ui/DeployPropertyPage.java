package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jface.databinding.preference.PreferencePageSupport;
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
import com.google.cloud.tools.eclipse.ui.util.databinding.BooleanConverter;
import com.google.cloud.tools.eclipse.ui.util.databinding.BucketNameValidator;
import com.google.cloud.tools.eclipse.ui.util.databinding.ProjectIdValidator;
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

  private Button promptForProjectIdButton;
  private Label projectIdLabel;
  private Text projectId;

  private Button overrideDefaultVersionButton;
  private Label versionLabel;
  private Text version;

  private Button autoPromoteButton;

  private Button overrideDefaultBucketButton;
  private Label bucketLabel;
  private Text bucket;

  private Model model = new Model();

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    createProjectIdSection(container);

    createProjectVersionSection(container);

    createPromoteSection(container);

    createAdvancedSection(container);

    Dialog.applyDialogFont(container);

    setupDataBinding();

    loadPreferences();

    return container;
  }

  private void setupDataBinding() {
    DataBindingContext dbc = new DataBindingContext();

    setupProjectIdDataBinding(dbc);
    setupProjectVersionDataBinding(dbc);
    setupAutoPromoteDataBinding(dbc);
    setupBucketDataBinding(dbc);

    PreferencePageSupport.create(this, dbc);
  }

  private void setupProjectIdDataBinding(DataBindingContext dbc) {
    ProjectIdValidator projectIdValidator = new ProjectIdValidator(new ProjectIdValidator.ValidationPredicate() {
      @Override
      public boolean shouldValidate() {
        return !model.isPromptForProjectId();
      }
    });

    dbc.bindValue(WidgetProperties.selection().observe(promptForProjectIdButton), model.promptForProjectId);
    dbc.bindValue(WidgetProperties.text(SWT.Modify).observe(projectId),
                  model.projectId,
                  new UpdateValueStrategy().setAfterGetValidator(projectIdValidator),
                  new UpdateValueStrategy().setAfterGetValidator(projectIdValidator));
    dbc.bindValue(model.promptForProjectId,
                  WidgetProperties.enabled().observe(projectId),
                  new UpdateValueStrategy().setConverter(BooleanConverter.negate()),
                  new UpdateValueStrategy().setConverter(BooleanConverter.negate()));
    dbc.bindValue(model.promptForProjectId,
                  WidgetProperties.enabled().observe(projectIdLabel),
                  new UpdateValueStrategy().setConverter(BooleanConverter.negate()),
                  new UpdateValueStrategy().setConverter(BooleanConverter.negate()));
  }

  private void setupProjectVersionDataBinding(DataBindingContext dbc) {
    dbc.bindValue(WidgetProperties.selection().observe(overrideDefaultVersionButton), model.overrideDefaultVersioning);
    dbc.bindValue(WidgetProperties.text(SWT.Modify).observe(version), model.version);
    dbc.bindValue(model.overrideDefaultVersioning, WidgetProperties.enabled().observe(versionLabel));
    dbc.bindValue(model.overrideDefaultVersioning, WidgetProperties.enabled().observe(version));
  }

  private void setupAutoPromoteDataBinding(DataBindingContext dbc) {
    dbc.bindValue(WidgetProperties.selection().observe(autoPromoteButton), model.autoPromote);
  }

  private void setupBucketDataBinding(DataBindingContext dbc) {
    dbc.bindValue(WidgetProperties.selection().observe(overrideDefaultBucketButton), model.overrideDefaultBucket);
    BucketNameValidator bucketNameValidator = new BucketNameValidator(new BucketNameValidator.ValidationPredicate() {
      @Override
      public boolean shouldValidate() {
        return model.isOverrideDefaultBucket();
      }
    });
    dbc.bindValue(WidgetProperties.text(SWT.Modify).observe(bucket),
                  model.bucket,
                  new UpdateValueStrategy().setAfterGetValidator(bucketNameValidator),
                  new UpdateValueStrategy().setAfterGetValidator(bucketNameValidator));
    dbc.bindValue(model.overrideDefaultBucket, WidgetProperties.enabled().observe(bucketLabel));
    dbc.bindValue(model.overrideDefaultBucket, WidgetProperties.enabled().observe(bucket));
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

    ScopedPreferenceStore scopedPreferenceStore = (ScopedPreferenceStore)preferenceStore;
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

    promptForProjectIdButton = new Button(projectIdComp, SWT.CHECK);
    promptForProjectIdButton.setText(Messages.getString("deploy.prompt.projectid"));
    promptForProjectIdButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

    projectIdLabel = new Label(projectIdComp, SWT.LEFT);
    GridData layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    layoutData.horizontalIndent = INDENT_CHECKBOX_ENABLED_WIDGET;
    projectIdLabel.setLayoutData(layoutData);
    projectIdLabel.setText(Messages.getString("project.id"));

    projectId = new Text(projectIdComp, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    projectId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
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
        setMessage(Messages.getString("cannot.open.browser", ex.getLocalizedMessage()), IMessageProvider.WARNING);
      }
    }));
  }

  private void createAdvancedSection(Composite parent) {
    ExpandableComposite expandableComposite = createExpandableComposite(parent);
    Composite defaultBucketComp = createBucketSection(expandableComposite);
    expandableComposite.setClient(defaultBucketComp);
  }

  private ExpandableComposite createExpandableComposite(Composite parent) {
    ExpandableComposite expandableComposite =
        new ExpandableComposite(parent, SWT.NONE, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
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

    private WritableValue promptForProjectId = new WritableValue(); //boolean
    private WritableValue projectId = new WritableValue(); //string
    private WritableValue overrideDefaultVersioning = new WritableValue(); //boolean
    private WritableValue version = new WritableValue(); //string
    private WritableValue autoPromote = new WritableValue(); //boolean
    private WritableValue overrideDefaultBucket = new WritableValue(); //boolean
    private WritableValue bucket = new WritableValue(); //string

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
}
