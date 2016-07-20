package com.google.cloud.tools.eclipse.appengine.login.ui;

import java.net.URL;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class LoginBrowser extends Dialog {

  private static final String LOGOUT_URL = "https://www.google.com/accounts/Logout"; //$NON-NLS-1$
  private static final String SUCCESS_CODE_PREFIX = "Success code="; //$NON-NLS-1$

  private Browser browser;
  private URL loginUrl;

  private String verificationCode;

  public LoginBrowser(IShellProvider shellProvider, URL loginUrl) {
    super(shellProvider.getShell());
    this.loginUrl = loginUrl;
  }

  public String getVerificationCode() {
    return verificationCode;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Sign in to Google");  // TODO(chanseok): localize after PR #386 lands.
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite composite = (Composite) super.createDialogArea(parent);

    browser = new Browser(composite, SWT.BORDER);
    browser.setUrl(loginUrl.toString());
    browser.addProgressListener(new PageLoadingListener());
    browser.addTitleListener(new TitleChangeListener());
    GridDataFactory.fillDefaults().grab(true, true).hint(1060, 660).applyTo(browser);

    return composite;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  @Override
  protected boolean isResizable() {
    return true;
  }

  /**
   * To capture verification code that will be returned as a title.
   */
  private class TitleChangeListener implements TitleListener {
    @Override
    public void changed(TitleEvent event) {
      if (event.title.startsWith(SUCCESS_CODE_PREFIX)) {
        verificationCode = event.title.substring(SUCCESS_CODE_PREFIX.length());
        // We don't close the browser now; rather we make the browser log out the user first.
        browser.setUrl(LOGOUT_URL);
      }
    }
  };

  /**
   *  To close the login browser after verifying that the browser is in the logged-out state.
   *  (Otherwise, the next time the login browser is presented, the browser may already be in
   *  the logged-in state.)
   */
  private class PageLoadingListener extends ProgressAdapter {
    @Override
    public void completed(ProgressEvent event) {
      if (verificationCode != null) {
        close();
      }
    }
  };
}
