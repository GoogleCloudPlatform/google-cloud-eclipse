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

    new Label(composite, SWT.LEAD).setText("Account:");
    accountSelector = new AccountSelector(composite, loginService);

    new Label(composite, SWT.LEAD).setText("Project:");
    projectSelector = new ProjectSelector(composite);

    new Label(composite, SWT.LEAD).setText("Service key:");
    serviceKeyInput = new Text(composite, SWT.BORDER);
    Button browse = new Button(composite, SWT.NONE);
    browse.setText("Browse...");

    GridDataFactory.fillDefaults().span(2, 1).applyTo(accountSelector);
    GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(projectSelector);
    GridLayoutFactory.swtDefaults().numColumns(3).generateLayout(composite);

    addDialogUpdateOnInputChangeListeners();

    setControl(composite);

    gcpIcon = SharedImages.GCP_IMAGE_DESCRIPTOR.createImage();
    composite.addDisposeListener(new ImageDisposer(gcpIcon));
  }

  private void addDialogUpdateOnInputChangeListeners() {
    // Updates buttons (e.g., enable/disable "Apply"), messages, the dirty status whenever users
    // makes changes.
    accountSelector.addSelectionListener(new Runnable() {
      @Override
      public void run() {
        updateLaunchConfigurationDialog();
      }
    });
    serviceKeyInput.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        updateLaunchConfigurationDialog();
      }
    });
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    // No particular default values to set in a newly created configuration.
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    String accountEmail = getAttribute(configuration, ATTRIBUTE_ACCOUNT_EMAIL, "");
    String projectId = getAttribute(configuration, ATTRIBUTE_GCP_PROJECT, "");
    String serviceKey = getAttribute(configuration, ATTRIBUTE_SERVICE_KEY, "");

    System.out.println("initializeForm: " + configuration.getName());
    System.out.println("    config: email=" + accountEmail + ", key=" + serviceKey);
    System.out.println("    UI: email=" + accountSelector.getSelectedEmail() + ", key=" + serviceKeyInput.getText());

    projectId = "chanseok-playground-new";
    accountSelector.selectAccount(accountEmail);
    serviceKeyInput.setText(serviceKey);

    if (accountSelector.getSelectedEmail().isEmpty()) {
      try {
        Credential credential = loginService.getCredential(accountEmail);
        GcpProject gcpProject;
        gcpProject = projectRepository.getProject(credential, projectId);
        projectSelector.setSelection(new StructuredSelection(gcpProject));
      } catch (ProjectRepositoryException e) {
        logger.log(Level.WARNING, "Could not retrieve GCP project information.", e);
      }
    }
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
    if (!savedAccountEmail.equals(selectedEmail)) {
      if (!selectedEmail.isEmpty()) {  // Some email was selected; save with no doubt
        configuration.setAttribute(ATTRIBUTE_ACCOUNT_EMAIL, selectedEmail);
      // Otherwise, it's a request to clear the saved email, *unless* the saved email could not be
      // selected from the beginning because it was not logged in.
      } else if (accountSelector.isEmailAvailable(savedAccountEmail)) {
        configuration.setAttribute(ATTRIBUTE_ACCOUNT_EMAIL, selectedEmail);
      }
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
