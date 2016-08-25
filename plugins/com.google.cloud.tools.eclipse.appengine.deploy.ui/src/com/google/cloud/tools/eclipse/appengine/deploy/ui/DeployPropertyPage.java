
package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.ObservablesManager;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.databinding.preference.PreferencePageSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
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
import com.google.common.base.Preconditions;

public class DeployPropertyPage extends PropertyPage {

  private static final String APPENGINE_DASHBOARD_URL = "https://console.cloud.google.com/appengine";

  private static final int INDENT_CHECKBOX_ENABLED_WIDGET = 10;

  private static final String PREFERENCE_STORE_DEFAULTS_QUALIFIER =
      "com.google.cloud.tools.eclipse.appengine.deploy.defaults";
  private static final String PREFERENCE_STORE_QUALIFIER = "com.google.cloud.tools.eclipse.appengine.deploy";
  private static final String PREF_PROMPT_FOR_PROJECT_ID = "project.id.promptOnDeploy"; // boolean
  private static final String PREF_PROJECT_ID = "project.id";
  private static final String PREF_OVERRIDE_DEFAULT_VERSIONING = "project.version.overrideDefault"; // boolean
  private static final String PREF_CUSTOM_VERSION = "project.version";
  private static final String PREF_ENABLE_AUTO_PROMOTE = "project.promote"; // boolean
  private static final String PREF_OVERRIDE_DEFAULT_BUCKET = "project.bucket.overrideDefault"; // boolean
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


  public DeployPropertyPage() {
    DefaultScope.INSTANCE.getNode(PREFERENCE_STORE_DEFAULTS_QUALIFIER).putBoolean(PREF_PROMPT_FOR_PROJECT_ID, true);
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(1, false);
    // set margin to 0 to meet expectations of super.createContents(Composite)
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    container.setLayout(layout);

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
    observables = new ObservablesManager();
    observables.addObservablesFromContext(bindingContext, true, true);
  }

  private void setupProjectIdDataBinding(DataBindingContext context) {
    ISWTObservableValue promptObservable = WidgetProperties.selection().observe(promptForProjectIdButton);
    ISWTObservableValue projectIdObservable = WidgetProperties.text(SWT.Modify).observe(projectId);

    context.bindValue(promptObservable, model.promptForProjectId);

    context.addValidationStatusProvider(new ProjectIdMultiValidator(promptObservable, projectIdObservable));
  }

  private void setupProjectVersionDataBinding(DataBindingContext context) {
    final ISWTObservableValue overrideObservable = WidgetProperties.selection().observe(overrideDefaultVersionButton);
    final ISWTObservableValue versionObservable = WidgetProperties.text(SWT.Modify).observe(version);

    context.bindValue(overrideObservable, model.overrideDefaultVersioning);
    context.bindValue(versionObservable, model.version);
    context.bindValue(WidgetProperties.enabled().observe(versionLabel), model.overrideDefaultVersioning);
    context.bindValue(WidgetProperties.enabled().observe(version), model.overrideDefaultVersioning);

    context.addValidationStatusProvider(new OverrideValidator(overrideObservable,
                                                              versionObservable,
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
    if (isValid()) {
      try {
        savePreferences();
        return true;
      } catch (IOException exception) {
        logger.log(Level.SEVERE, "Could not save deploy preferences", exception);
        MessageDialog.openError(getShell(),
                                Messages.getString("deploy.preferences.save.error.title"),
                                Messages.getString("deploy.preferences.save.error.message",
                                                   exception.getLocalizedMessage()));
      }
    }
    return false;
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
    return new ScopedPreferenceStore(new ProjectScope(project),
                                     PREFERENCE_STORE_QUALIFIER,
                                     PREFERENCE_STORE_DEFAULTS_QUALIFIER);
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
    promptForProjectIdButton.setText(Messages.getString("deploy.prompt.projectid"));
    GridData layoutData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    layoutData.horizontalIndent = INDENT_CHECKBOX_ENABLED_WIDGET;
    promptForProjectIdButton.setLayoutData(layoutData);

    ControlDecoration controlDecoration = new ControlDecoration(promptForProjectIdButton, SWT.RIGHT | SWT.TOP);
    controlDecoration.setDescriptionText(Messages.getString("deploy.prompt.projectid.long"));
    Image decorationImage = getInfoDecorationImage();
    controlDecoration.setImage(decorationImage);
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

  private Image getInfoDecorationImage() {
    return FieldDecorationRegistry.getDefault()
        .getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage();
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

  /**
   * Validates a checkbox and text field as follows:
   * <ol>
   * <li>if the checkbox is unselected -> the text field cannot contain the empty string
   * <li>if the checkbox is selected -> the text field can contain the empty string
   * <li>if the text field is not empty, the result is determined by {@link ProjectIdValidator} applied to the value
   * </ol>
   */
  private static class ProjectIdMultiValidator extends MultiValidator {

    private ISWTObservableValue selectionObservable;
    private ISWTObservableValue textObservable;
    private ProjectIdValidator validator = new ProjectIdValidator();

    /**
     * @param selection must be an observable for a checkbox, i.e. a {@link Button} with {@link SWT#CHECK} style
     * @param text must be an observable for a {@link Text}
     */
    public ProjectIdMultiValidator(ISWTObservableValue selection, ISWTObservableValue text) {
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
    }

    @Override
    protected IStatus validate() {
      if (Boolean.FALSE.equals(selectionObservable.getValue())) {
        return validator.validate(textObservable.getValue(), ProjectIdValidator.ValidationPolicy.EMPTY_IS_INVALID);
      }
      return validator.validate(textObservable.getValue(), ProjectIdValidator.ValidationPolicy.EMPTY_IS_VALID);
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
}
