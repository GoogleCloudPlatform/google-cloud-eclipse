package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.api.client.auth.oauth2.Credential;
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

public class GcpEmulationTab extends AbstractLaunchConfigurationTab {

  private static final Logger logger = Logger.getLogger(GcpEmulationTab.class.getName());

  private static final String ATTRIBUTE_ACCOUNT_EMAIL =
      "com.google.cloud.tools.eclipse.gcp.emulation.accountEmail";

  private static final String ATTRIBUTE_GCP_PROJECT =
      "com.google.cloud.tools.eclipse.gcp.emulation.gcpProject";

  private static final String ATTRIBUTE_SERVICE_KEY =
      "com.google.cloud.tools.eclipse.gcp.emulation.serviceKey";

  private final IGoogleLoginService loginService;
  private final ProjectRepository projectRepository;

  private AccountSelector accountSelector;
  private ProjectSelector projectSelector;
  private Text serviceKeyInput;

  private Image gcpIcon;

  private boolean initializingUiValues;
  // We set up intermediary models between a run configuration and UI components for certain values,
  // because, e.g., the account selector cannot load an email if it is not logged in. In such a
  // case, although nothing is selected in the account selector, we should not clear the email saved
  // in the run configuration.
  private String accountEmailModel;
  private String gcpProjectIdModel;

  public GcpEmulationTab() {
    this(PlatformUI.getWorkbench().getService(IGoogleLoginService.class),
        new ProjectRepository(PlatformUI.getWorkbench().getService(IGoogleApiFactory.class)));
  }

  @VisibleForTesting
  GcpEmulationTab(IGoogleLoginService loginService, ProjectRepository projectRepository) {
    this.loginService = loginService;
    this.projectRepository = projectRepository;
  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    // Account row
    new Label(composite, SWT.LEAD).setText("Account:");
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
            gcpProjectIdModel = "";
            updateLaunchConfigurationDialog();
          }
        }
      }
    });

    // Project row
    Label projectLabel = new Label(composite, SWT.LEAD);
    projectLabel.setText("Project:");

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

    filterField.setMessage("Filter projects by name or ID");
    filterField.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        projectSelector.setFilter(filterField.getText());
      }
    });

    // Service key row
    new Label(composite, SWT.LEAD).setText("Service key:");
    serviceKeyInput = new Text(composite, SWT.BORDER);
    serviceKeyInput.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        updateLaunchConfigurationDialog();
      }
    });
    Button browse = new Button(composite, SWT.NONE);
    browse.setText("Browse...");

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
    String serviceKey = getAttribute(configuration, ATTRIBUTE_SERVICE_KEY, "");
    accountEmailModel = getAttribute(configuration, ATTRIBUTE_ACCOUNT_EMAIL, "");
    gcpProjectIdModel = getAttribute(configuration, ATTRIBUTE_GCP_PROJECT, "");

    initializingUiValues = true;
    serviceKeyInput.setText(serviceKey);
    accountSelector.selectAccount(accountEmailModel);
    projectSelector.selectProjectId(gcpProjectIdModel);
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
        logger.log(Level.WARNING, "Could not retrieve GCP project information from server.", e);
      }
    }
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    configuration.setAttribute(ATTRIBUTE_SERVICE_KEY, serviceKeyInput.getText());
    configuration.setAttribute(ATTRIBUTE_ACCOUNT_EMAIL, accountEmailModel);
    configuration.setAttribute(ATTRIBUTE_GCP_PROJECT, gcpProjectIdModel);
  }

  @Override
  public String getName() {
    return "GCP";
  }

  @Override
  public Image getImage() {
    return gcpIcon;
  }

  private static final String getAttribute(ILaunchConfiguration configuration,
      String attribute, String defaultValue) {
    try {
      return configuration.getAttribute(attribute, defaultValue);
    } catch (CoreException e) {
      logger.log(Level.WARNING, "Can't get value from launch configuration.", e);
      return defaultValue;
    }
  }
}
