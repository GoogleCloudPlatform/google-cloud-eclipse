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
import org.eclipse.jface.viewers.StructuredSelection;
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

  private String savedGcpProjectId;

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
      }
    });

    // Project row
    Label projectLabel = new Label(composite, SWT.LEAD);
    projectLabel.setText("Project:");

    Composite projectSelectorComposite = new Composite(composite, SWT.NONE);
    final Text filterField = new Text(projectSelectorComposite,
        SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);

    projectSelector = new ProjectSelector(projectSelectorComposite);

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
    String accountEmail = getAttribute(configuration, ATTRIBUTE_ACCOUNT_EMAIL, "");
    savedGcpProjectId = getAttribute(configuration, ATTRIBUTE_GCP_PROJECT, "");
    String serviceKey = getAttribute(configuration, ATTRIBUTE_SERVICE_KEY, "");

    System.out.println("initializeForm: " + configuration.getName());
    System.out.println("    config: email=" + accountEmail + ", key=" + serviceKey);
    System.out.println("    UI: email=" + accountSelector.getSelectedEmail() + ", key=" + serviceKeyInput.getText());

    accountSelector.selectAccount(accountEmail);
    serviceKeyInput.setText(serviceKey);

    updateProjectSelector();
  }

  private void updateProjectSelector() {
    Credential credential = accountSelector.getSelectedCredential();
    if (credential == null) {
      projectSelector.setProjects(new ArrayList<GcpProject>());
    } else {
      try {
        List<GcpProject> gcpProjects = projectRepository.getProjects(credential);
        projectSelector.setProjects(gcpProjects);

        if (!savedGcpProjectId.isEmpty()) {
          GcpProject gcpProject = projectRepository.getProject(credential, savedGcpProjectId);
          projectSelector.setSelection(new StructuredSelection(gcpProject));
        }
      } catch (ProjectRepositoryException e) {
        logger.log(Level.WARNING, "Could not retrieve GCP project information from server.", e);
      }
    }
    updateLaunchConfigurationDialog();

    projectSelector.isProjectIdAvailable(savedGcpProjectId);
    System.out.println("Can select [" + savedGcpProjectId + "].");
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    String savedAccountEmail = getAttribute(configuration, ATTRIBUTE_ACCOUNT_EMAIL, "");
    String savedServiceKey = getAttribute(configuration, ATTRIBUTE_SERVICE_KEY, "");

    System.out.println("performApply: " + configuration.getName());
    System.out.println("    config: email=" + savedAccountEmail + ", key=" + savedServiceKey);
    System.out.println("    UI: email=" + accountSelector.getSelectedEmail() + ", key=" + serviceKeyInput.getText());

    // We want to call "setAttribute()" only when really needed; otherwise, Eclipse asks users to
    // confirm if they want to save non-existential "changes".
    if (!savedServiceKey.equals(serviceKeyInput.getText())) {
      configuration.setAttribute(ATTRIBUTE_SERVICE_KEY, serviceKeyInput.getText());
    }

    String selectedEmail = accountSelector.getSelectedEmail();
    if (selectedEmail.isEmpty()) {
      if (!savedAccountEmail.isEmpty()) {
        // Some email was saved, and now no email is selected: this means either 1) the user
        // explicitly unselected it to clear the saved email; or 2) the email couldn't be selected
        // simply because it was not logged in. Don't clear the saved email in the last case.
        if (accountSelector.isEmailAvailable(savedAccountEmail)) {
          configuration.setAttribute(ATTRIBUTE_ACCOUNT_EMAIL, "");
        }
      }
    } else if (!savedAccountEmail.equals(selectedEmail)) {
      configuration.setAttribute(ATTRIBUTE_ACCOUNT_EMAIL, selectedEmail);
    }

    GcpProject gcpProject = (GcpProject) projectSelector.getSelection().getFirstElement();
    if (gcpProject == null) {
      if (!savedGcpProjectId.isEmpty()) {
        // Some project ID was saved, and now no project is selected: this means either 1) the user
        // explicitly unselected it to clear the saved project; 2) the user selected another
        // account (or cleared the current one); or 3) the project couldn't be selected simply
        // because the project selector did not have a credential to list projects (i.e., the
        // required account was not logged in). Don't clear the saved project in the last case.
        String updatedAccountEmail = getAttribute(configuration, ATTRIBUTE_ACCOUNT_EMAIL, "");
        boolean emailChanged = savedAccountEmail.equals(updatedAccountEmail);

        if (emailChanged || projectSelector.isProjectIdAvailable(savedGcpProjectId)) {
          configuration.setAttribute(ATTRIBUTE_GCP_PROJECT, "");
        }
      }
    } else if (!savedGcpProjectId.equals(gcpProject.getId())) {
      configuration.setAttribute(ATTRIBUTE_GCP_PROJECT, gcpProject.getId());
    }
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
