/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.localserver.Messages;
import com.google.cloud.tools.eclipse.appengine.localserver.ServiceAccountUtil;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.ui.util.event.FileFieldSetter;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class GcpLocalRunTab extends AbstractLaunchConfigurationTab {

  private static final Logger logger = Logger.getLogger(GcpLocalRunTab.class.getName());

  private static final String ATTRIBUTE_ACCOUNT_EMAIL =
      "com.google.cloud.tools.eclipse.gcpEmulation.accountEmail"; //$NON-NLS-1$

  private static final String PROJECT_ID_ENVIRONMENT_VARIABLE =
      "GOOGLE_CLOUD_PROJECT"; //$NON-NLS-1$
  private static final String SERVICE_KEY_ENVIRONMENT_VARIABLE =
      "GOOGLE_APPLICATION_CREDENTIALS"; //$NON-NLS-1$

  private Image gcpIcon;

  private final EnvironmentTab environmentTab;
  private final ProjectRepository projectRepository;

  private AccountSelector accountSelector;
  private ProjectSelector projectSelector;
  private Text serviceKeyInput;
  private Button createServiceKey;
  @VisibleForTesting
  ControlDecoration serviceKeyDecoration;

  // We set up intermediary models between a run configuration and UI components for certain values,
  // because, e.g., the account selector cannot load an email if it is not logged in. In such a
  // case, although nothing is selected in the account selector, we should not clear the email saved
  // in the run configuration.
  private String accountEmailModel;
  private String gcpProjectIdModel;
  // To prevent updating above models when programmatically setting up UI components.
  private boolean initializingUiValues;
  
  /**
   * True if this tab is the currently visible tab. See {@link
   * #performApply(ILaunchConfigurationWorkingCopy)} for details.
   *
   * @see #performApply(ILaunchConfigurationWorkingCopy)
   */
  private boolean activated;

  public GcpLocalRunTab(EnvironmentTab environmentTab) {
    this(environmentTab,
        new ProjectRepository());
  }

  @VisibleForTesting
  GcpLocalRunTab(EnvironmentTab environmentTab, ProjectRepository projectRepository) {
    this.environmentTab = environmentTab;
    this.projectRepository = projectRepository;
  }

  @Override
  public String getName() {
    return Messages.getString("gcp.local.run.tab.name"); //$NON-NLS-1$
  }

  @Override
  public Image getImage() {
    if (gcpIcon == null) {
      gcpIcon = SharedImages.GCP_IMAGE_DESCRIPTOR.createImage();
    }
    return gcpIcon;
  }

  @Override
  public void dispose() {
    if (gcpIcon != null) {
      gcpIcon.dispose();
      gcpIcon = null;
    }
  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    // Account row
    new Label(composite, SWT.LEAD).setText(Messages.getString("label.account")); //$NON-NLS-1$
    accountSelector = new AccountSelector(composite);
    accountEmailModel = accountSelector.getSelectedEmail();
    accountSelector.addSelectionListener(() -> {
      updateProjectSelector();

      if (!initializingUiValues) {
        boolean accountSelected = !accountSelector.getSelectedEmail().isEmpty();
        boolean savedEmailAvailable = accountSelector.isEmailAvailable(accountEmailModel);
        // 1. If some account is selected, always save it.
        // 2. Otherwise (no account selected), clear the saved email only when it is certain
        // that the user explicitly removed selection (i.e., not because of logout).
        if (accountSelected || savedEmailAvailable) {
          accountEmailModel = accountSelector.getSelectedEmail();
          gcpProjectIdModel = ""; //$NON-NLS-1$
          updateLaunchConfigurationDialog();
        }
      }
    });

    // Project row
    Label projectLabel = new Label(composite, SWT.LEAD);
    projectLabel.setText(Messages.getString("label.project")); //$NON-NLS-1$

    Composite projectSelectorComposite = new Composite(composite, SWT.NONE);
    Text filterField = new Text(projectSelectorComposite,
        SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);

    projectSelector = new ProjectSelector(projectSelectorComposite);
    projectSelector.addSelectionChangedListener(event -> {
      boolean projectSelected = !projectSelector.getSelection().isEmpty();
      createServiceKey.setEnabled(projectSelected);

      if (!initializingUiValues) {
        boolean savedIdAvailable = projectSelector.isProjectIdAvailable(gcpProjectIdModel);
        // 1. If some project is selected, always save it.
        // 2. Otherwise (no project selected), clear the saved project only when it is certain
        // that the user explicitly removed selection (i.e., not because of logout).
        if (projectSelected || savedIdAvailable) {
          gcpProjectIdModel = projectSelector.getSelectedProjectId();
          updateLaunchConfigurationDialog();
        }
      }
    });

    filterField.setMessage(Messages.getString("project.filter.hint")); //$NON-NLS-1$
    filterField.addModifyListener(event -> {
      projectSelector.setFilter(filterField.getText());
    });
    projectSelector.clearStatusLink();

    // Service key row
    Label serviceKeyLabel = new Label(composite, SWT.LEAD);
    serviceKeyLabel.setText(Messages.getString("label.service.key")); //$NON-NLS-1$
    
    serviceKeyInput = new Text(composite, SWT.BORDER);
    serviceKeyInput.addModifyListener(event -> {
      serviceKeyDecoration.hide();
      updateLaunchConfigurationDialog();
    });
    Button browse = new Button(composite, SWT.NONE);
    browse.setText(Messages.getString("button.browse")); //$NON-NLS-1$
    String[] filterExtensions = new String[] {"*.json"}; //$NON-NLS-1$
    browse.addSelectionListener(new FileFieldSetter(serviceKeyInput, filterExtensions));

    serviceKeyDecoration = new ControlDecoration(serviceKeyInput, SWT.LEAD | SWT.TOP);
    serviceKeyDecoration.hide();

    createServiceKey = new Button(composite, SWT.NONE);
    createServiceKey.setText(Messages.getString("create.new.service.key")); //$NON-NLS-1$
    createServiceKey.setToolTipText(
        Messages.getString("create.new.service.key.tooltip")); //$NON-NLS-1$
    createServiceKey.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        BusyIndicator.showWhile(createServiceKey.getDisplay(), () -> {
          Path keyPath = getServiceAccountKeyPath();
          if (keyPath != null) {
            createServiceAccountKey(keyPath);
          }
        });
      }
    });

    GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.TOP).applyTo(projectLabel);
    GridDataFactory.fillDefaults().span(3, 1).applyTo(accountSelector);
    GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER).applyTo(createServiceKey);

    GridDataFactory.fillDefaults().span(3, 1).applyTo(projectSelectorComposite);
    GridDataFactory.fillDefaults().applyTo(filterField);
    GridDataFactory.fillDefaults().grab(true, false).hint(300, 200)
        .applyTo(projectSelector);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false)
        .applyTo(serviceKeyInput);

    GridLayoutFactory.fillDefaults().spacing(0, 0).generateLayout(projectSelectorComposite);
    GridLayoutFactory.swtDefaults().numColumns(4).generateLayout(composite);

    setControl(composite);
    updateProjectSelector();
  }

  private void updateProjectSelector() {
    Credential credential = accountSelector.getSelectedCredential();
    if (credential == null) {
      projectSelector.setProjects(new ArrayList<GcpProject>());
      return;
    }

    BusyIndicator.showWhile(projectSelector.getDisplay(), () -> {
      try {
        List<GcpProject> gcpProjects = projectRepository.getProjects();
        projectSelector.setProjects(gcpProjects);
      } catch (ProjectRepositoryException e) {
        logger.log(Level.WARNING,
            "Could not retrieve GCP project information from server.", e); //$NON-NLS-1$
      }
    });
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    // No particular default values to set in a newly created configuration.
  }

  @Override
  public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
    activated = true;
    super.activated(workingCopy);
  }

  @Override
  public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
    super.deactivated(workingCopy);
    activated = false;
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    Map<String, String> environmentMap = getEnvironmentMap(configuration);
    gcpProjectIdModel = Strings.nullToEmpty(environmentMap.get(PROJECT_ID_ENVIRONMENT_VARIABLE));
    String serviceKey = Strings.nullToEmpty(environmentMap.get(SERVICE_KEY_ENVIRONMENT_VARIABLE));

    initializingUiValues = true;
    projectSelector.selectProjectId(gcpProjectIdModel);
    serviceKeyInput.setText(serviceKey);
    initializingUiValues = false;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    // Must avoid updating the environment map unless we're active as performApply() is also called
    // whenever the user makes changes in other tabs, like the EnvironmentTab, and thus we could
    // clobber changes made in the EnvironmentTab.
    // (https://github.com/GoogleCloudPlatform/google-cloud-eclipse/pull/2568#discussion_r150128582)
    if (!activated) {
      return;
    }

    if (accountEmailModel.isEmpty()) {
      configuration.setAttribute(ATTRIBUTE_ACCOUNT_EMAIL, (String) null);
    } else {
      configuration.setAttribute(ATTRIBUTE_ACCOUNT_EMAIL, accountEmailModel);
    }

    Map<String, String> environmentMap = getEnvironmentMap(configuration);
    if (!gcpProjectIdModel.isEmpty()) {
      environmentMap.put(PROJECT_ID_ENVIRONMENT_VARIABLE, gcpProjectIdModel);
    } else {
      environmentMap.remove(PROJECT_ID_ENVIRONMENT_VARIABLE);
    }
    if (!serviceKeyInput.getText().isEmpty()) {
      environmentMap.put(SERVICE_KEY_ENVIRONMENT_VARIABLE, serviceKeyInput.getText());
    } else {
      environmentMap.remove(SERVICE_KEY_ENVIRONMENT_VARIABLE);
    }
    configuration.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, environmentMap);

    if (environmentTab != null) {
      // Unfortunately, "EnvironmentTab" overrides "activated()" not to call "initializeFrom()".
      // (Calling "initializeFrom()" when "activated()" is the default behavior of the base class
      // retained for backward compatibility.) We need to call it on behalf of "EnvironmentTab"
      // to re-initialize its UI with the changes made here.
      environmentTab.initializeFrom(configuration);
      // To make the dirty status correct, we also need to notify "EnvironmentTab" of the potential
      // changes made here to the launch config working copy.
      environmentTab.performApply(configuration);
    }
  }

  @Override
  public boolean isValid(ILaunchConfiguration configuration) {
    setErrorMessage(null);

    Map<String, String> environmentMap = getEnvironmentMap(configuration);
    String serviceKey = environmentMap.get(SERVICE_KEY_ENVIRONMENT_VARIABLE);
    if (!Strings.isNullOrEmpty(serviceKey)) {
      java.nio.file.Path path = Paths.get(serviceKey);
      if (!Files.exists(path)) {
        setErrorMessage(Messages.getString("error.file.does.not.exist", serviceKey)); //$NON-NLS-1$
        return false;
      } else if (Files.isDirectory(path)) {
        setErrorMessage(Messages.getString("error.is.a.directory", serviceKey)); //$NON-NLS-1$
        return false;
      } else if (!Files.isReadable(path)) {
        setErrorMessage(Messages.getString("error.is.not.readable", serviceKey)); //$NON-NLS-1$
        return false;
      }
    }
    return true;
  }

  @VisibleForTesting
  static String getAttribute(ILaunchConfiguration configuration,
      String attribute, String defaultValue) {
    try {
      return configuration.getAttribute(attribute, defaultValue);
    } catch (CoreException e) {
      logger.log(Level.WARNING, "Can't get value from launch configuration.", e); //$NON-NLS-1$
      return defaultValue;
    }
  }

  @VisibleForTesting
  static Map<String, String> getEnvironmentMap(ILaunchConfiguration configuration) {
    // Don't return an immutable map such as Collections.emptyMap().
    Map<String, String> emptyMap = new HashMap<>();
    try {
      return configuration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, emptyMap);
    } catch (CoreException e) {
      logger.log(Level.WARNING, "Can't get value from launch configuration.", e); //$NON-NLS-1$
      return emptyMap;
    }
  }

  @VisibleForTesting
  Path getServiceAccountKeyPath() {
    Preconditions.checkState(!projectSelector.getSelection().isEmpty());

    String projectId = projectSelector.getSelectedProjectId();
    String filename = "app-engine-default-service-account-key-" //$NON-NLS-1$
        + projectId + ".json"; //$NON-NLS-1$
    
    // can't use colons in filenames on Windows
    filename = filename.replace(':', '.');

    try {
      Path configurationLocation = Paths.get(Platform.getConfigurationLocation().getURL().toURI());
      return configurationLocation.resolve("com.google.cloud.tools.eclipse") //$NON-NLS-1$
          .resolve(filename);
    } catch (URISyntaxException e) {
      logger.log(Level.SEVERE, "Cannot convert configuration location into URI", e); //$NON-NLS-1$
      return null;
    }
  }

  @VisibleForTesting
  void createServiceAccountKey(Path keyFile) {
    String projectId = projectSelector.getSelectedProjectId();
    Preconditions.checkState(GoogleApiFactory.INSTANCE.getCredential().isPresent(), "no account selected"); //$NON-NLS-1$
    Preconditions.checkState(!projectId.isEmpty(), "no project selected"); //$NON-NLS-1$
    
    try { 
      ServiceAccountUtil.createAppEngineDefaultServiceAccountKey(projectId, keyFile);

      serviceKeyInput.setText(keyFile.toString());
      String message = Messages.getString("service.key.created", keyFile); //$NON-NLS-1$
      showServiceKeyDecorationMessage(message, false /* isError */);

    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      String message = Messages.getString("cannot.create.service.key", //$NON-NLS-1$
          e.getLocalizedMessage());
      showServiceKeyDecorationMessage(message, true /* isError */);
    }
  }

  private void showServiceKeyDecorationMessage(String message, boolean isError) {
    FieldDecorationRegistry registry = FieldDecorationRegistry.getDefault();
    FieldDecoration fieldDecoration = registry.getFieldDecoration(
        isError ? FieldDecorationRegistry.DEC_ERROR : FieldDecorationRegistry.DEC_INFORMATION);

    serviceKeyDecoration.show();
    serviceKeyDecoration.setImage(fieldDecoration.getImage());
    serviceKeyDecoration.setDescriptionText(message);
    serviceKeyDecoration.showHoverText(message);
  }
}
