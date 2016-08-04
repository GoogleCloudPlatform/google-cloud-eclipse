package com.google.cloud.tools.eclipse.ui.util.console;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.program.Program;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.console.IHyperlink;

/**
 * {@link IHyperlink} implementation that uses {@link IWorkbenchBrowserSupport} to open the URL.
 */
public class BrowserSupportBasedHyperlink implements IHyperlink {

  static final Logger logger = Logger.getLogger(BrowserSupportBasedHyperlink.class.toString());
  private String url;


  BrowserSupportBasedHyperlink(String url) {
    this.url = url;
  }

  @Override
  public void linkExited() {
  }

  @Override
  public void linkEntered() {
  }

  @Override
  public void linkActivated() {
    try {
      IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
      browserSupport.createBrowser(null).openURL(new URL(url));
    } catch (PartInitException partInitException) {
      logger.log(Level.SEVERE, "Cannot open hyperlink using browser support, will try SWT's Program.launch(String)",
                 partInitException);
      if (!Program.launch(url)) {
        logger.log(Level.SEVERE, "Cannot open hyperlink using SWT's Program.launch(String)");
      }
    } catch (MalformedURLException malformedURLException) {
      logger.log(Level.SEVERE, "Cannot open hyperlink", malformedURLException);
    }
  }
}
