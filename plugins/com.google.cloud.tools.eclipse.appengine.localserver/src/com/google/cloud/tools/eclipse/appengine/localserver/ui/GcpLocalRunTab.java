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
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDisposer;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class GcpLocalRunTab extends AbstractLaunchConfigurationTab {

  private static final Logger logger = Logger.getLogger(GcpLocalRunTab.class.getName());

  private static final String ATTRIBUTE_ACCOUNT_EMAIL =
      "com.google.cloud.tools.eclipse.gcpEmulation.accountEmail"; //$NON-NLS-1$

  private static final String ATTRIBUTE_GCP_PROJECT =
      "com.google.cloud.tools.eclipse.gcpEmulation.gcpProject"; //$NON-NLS-1$

  private static final String ATTRIBUTE_SERVICE_KEY =
      "com.google.cloud.tools.eclipse.gcpEmulation.serviceKey"; //$NON-NLS-1$

  private final IGoogleLoginService loginService;
  private final ProjectRepository projectRepository;

  private AccountSelector accountSelector;
  private ProjectSelector projectSelector;
  private Text serviceKeyInput;

  private Image gcpIcon;

  // We set up intermediary models between a run configuration and UI components for certain values,
  // because, e.g., the account selector cannot load an email if it is not logged in. In such a
  // case, although nothing is selected in the account selector, we should not clear the email saved
  // in the run configuration.
  private String accountEmailModel;
  private String gcpProjectIdModel;
  // To prevent updating above models when programmatically setting up UI components.
  private boolean initializingUiValues;

  public GcpLocalRunTab() {
    this(PlatformUI.getWorkbench().getService(IGoogleLoginService.class),
        new ProjectRepository(PlatformUI.getWorkbench().getService(IGoogleApiFactory.class)));
  }

  @VisibleForTesting
  GcpLocalRunTab(IGoogleLoginService loginService, ProjectRepository projectRepository) {
    this.loginService = loginService;
    this.projectRepository = projectRepository;
  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    // Account row
    new Label(composite, SWT.LEAD).setText(Messages.getString("label.account")); //$NON-NLS-1$
    accountSelector = new AccountSelector(composite, loginService);
    accountSelector.addSelectionListener(new Runnable() {
      @Override
      public void run() {
        updateProjectSelector();

        if (!initializingUiValues) {
          boolean emptySelection = accountSelector.getSelectedEmail().isEmpty();
          boolean savedEmailAvailable = accountSelector.isEmailAvailable(accountEmailModel);
          if (!emptySelection || savedEmailAvailable) {
            accountEmailModel = accountSelector.getSelectedEmail();
            gcpProjectIdModel = ""; //$NON-NLS-1$
            updateLaunchConfigurationDialog();
          }
        }
      }
    });

    // Project row
    Label projectLabel = new Label(composite, SWT.LEAD);
    projectLabel.setText(Messages.getString("label.project")); //$NON-NLS-1$

    Composite projectSelectorComposite = new Composite(composite, SWT.NONE);
    final Text filterField = new Text(projectSelectorComposite,
        SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);

    projectSelector = new ProjectSelector(projectSelectorComposite);
    projectSelector.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        if (!initializingUiValues) {
          boolean emptySelection = projectSelector.getSelectProjectId().isEmpty();
          boolean savedIdAvailable = projectSelector.isProjectIdAvailable(gcpProjectIdModel);
          if (!emptySelection || savedIdAvailable) {
            gcpProjectIdModel = projectSelector.getSelectProjectId();
            updateLaunchConfigurationDialog();
          }
        }
      }
    });

    filterField.setMessage(Messages.getString("project.filter.hint")); //$NON-NLS-1$
    filterField.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        projectSelector.setFilter(filterField.getText());
      }
    });

    // Service key row
    new Label(composite, SWT.LEAD).setText(Messages.getString("label.service.key")); //$NON-NLS-1$
    serviceKeyInput = new Text(composite, SWT.BORDER);
    serviceKeyInput.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        updateLaunchConfigurationDialog();
      }
    });
    Button browse = new Button(composite, SWT.NONE);
    browse.setText(Messages.getString("button.browse")); //$NON-NLS-1$

    GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.TOP).applyTo(projectLabel);
    GridDataFactory.fillDefaults().span(2, 1).applyTo(accountSelector);
    GridLayoutFactory.swtDefaults().numColumns(3).generateLayout(composite);

    GridDataFactory.fillDefaults().span(2, 1).applyTo(projectSelectorComposite);
    GridDataFactory.fillDefaults().applyTo(filterField);
    GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 200)
        .applyTo(projectSelector);
    GridLayoutFactory.fillDefaults().spacing(0, 0).generateLayout(projectSelectorComposite);

    setControl(composite);

    gcpIcon = SharedImages.GCP_IMAGE_DESCRIPTOR.createImage();
    composite.addDisposeListener(new ImageDisposer(gcpIcon));
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    // No particular default values to set in a newly created configuration.
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    accountEmailModel = getAttribute(configuration, ATTRIBUTE_ACCOUNT_EMAIL, ""); //$NON-NLS-1$
    gcpProjectIdModel = getAttribute(configuration, ATTRIBUTE_GCP_PROJECT, ""); //$NON-NLS-1$
    String serviceKey = getAttribute(configuration, ATTRIBUTE_SERVICE_KEY, ""); //$NON-NLS-1$

    initializingUiValues = true;
    accountSelector.selectAccount(accountEmailModel);
    projectSelector.selectProjectId(gcpProjectIdModel);
    serviceKeyInput.setText(serviceKey);
    initializingUiValues = false;
  }

  private void updateProjectSelector() {
    Credential credential = accountSelector.getSelectedCredential();
    if (credential == null) {
      projectSelector.setProjects(new ArrayList<GcpProject>());
    } else {
      try {
        List<GcpProject> gcpProjects = projectRepository.getProjects(credential);
        projectSelector.setProjects(gcpProjects);
      } catch (ProjectRepositoryException e) {
        logger.log(Level.WARNING, "Could not retrieve GCP project information from server.", e); //$NON-NLS-1$
      }
    }
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    configuration.setAttribute(ATTRIBUTE_ACCOUNT_EMAIL, accountEmailModel);
    configuration.setAttribute(ATTRIBUTE_GCP_PROJECT, gcpProjectIdModel);
    configuration.setAttribute(ATTRIBUTE_SERVICE_KEY, serviceKeyInput.getText());
  }

  @Override
  public String getName() {
    return Messages.getString("gcp.emulation.tab.name"); //$NON-NLS-1$
  }

  @Override
  public Image getImage() {
    return gcpIcon;
  }

  @VisibleForTesting
  static final String getAttribute(ILaunchConfiguration configuration,
      String attribute, String defaultValue) {
    try {
      return configuration.getAttribute(attribute, defaultValue);
    } catch (CoreException e) {
      logger.log(Level.WARNING, "Can't get value from launch configuration.", e); //$NON-NLS-1$
      return defaultValue;
    }
  }
}
