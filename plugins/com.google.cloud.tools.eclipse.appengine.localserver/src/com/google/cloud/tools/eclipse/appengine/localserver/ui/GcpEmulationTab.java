package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDisposer;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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

  private Image gcpIcon;

  private AccountSelector accountSelector;
  private Text serviceKeyInput;

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    new Label(composite, SWT.LEAD).setText("Account:");
    IGoogleLoginService loginService =
        PlatformUI.getWorkbench().getService(IGoogleLoginService.class);
    accountSelector = new AccountSelector(composite, loginService);

    new Label(composite, SWT.LEAD).setText("Service key:");
    serviceKeyInput = new Text(composite, SWT.BORDER);
    Button browse = new Button(composite, SWT.NONE);
    browse.setText("Browse...");

    GridDataFactory.fillDefaults().span(2, 1).applyTo(accountSelector);
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
    String serviceKey = getAttribute(configuration, ATTRIBUTE_SERVICE_KEY, "");

    accountSelector.selectAccount(accountEmail);
    serviceKeyInput.setText(serviceKey);
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    String accountEmail = getAttribute(configuration, ATTRIBUTE_ACCOUNT_EMAIL, "");
    String serviceKey = getAttribute(configuration, ATTRIBUTE_SERVICE_KEY, "");

    // We want to call "setAttribute()" only when really needed; otherwise, Eclipse asks users to
    // confirm if they want to save non-existential "changes".
    if (!accountEmail.equals(accountSelector.getSelectedEmail())
        || !serviceKey.equals(serviceKeyInput.getText())) {
      configuration.setAttribute(ATTRIBUTE_ACCOUNT_EMAIL, accountSelector.getSelectedEmail());
      configuration.setAttribute(ATTRIBUTE_SERVICE_KEY, serviceKeyInput.getText());
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
