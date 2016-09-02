package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployJob;
import com.google.cloud.tools.eclipse.ui.util.Messages;

public class DeployConsolePageParticipant implements IConsolePageParticipant {

  private static final Logger logger = Logger.getLogger(DeployConsolePageParticipant.class.getName());

  private DeployConsole console;
  private Action terminateAction;
  private Action closeAction;

  @Override
  public void init(IPageBookViewPage page, IConsole console) {
    if (console instanceof DeployConsole) {
      this.console = (DeployConsole) console;
    } else {
      logger.log(Level.SEVERE,
                 "console is instance of {0}, expected was {1}",
                 new Object[]{ console.getClass().getName(), DeployConsole.class.getName() });
    }
    IActionBars actionBars = page.getSite().getActionBars();
    configureToolBar(actionBars.getToolBarManager());
    addJobchangeListener();
    update();
  }

  private void configureToolBar(IToolBarManager toolbarManager) {
    terminateAction = createTerminateAction();
    toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminateAction);

    closeAction = createCloseAction();
    toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, closeAction);
  }

  private void addJobchangeListener() {
    console.getJob().addJobChangeListener(new JobChangeAdapter() {
      public void done(IJobChangeEvent event) {
        System.out.println("done update");
        update();
      }
    });
  }

  private void update() {
    StandardDeployJob tag = console.getJob();

    if (terminateAction != null) {
      terminateAction.setEnabled(tag.getState() != Job.NONE);
    }

    if (closeAction != null) {
      closeAction.setEnabled(tag.getState() == Job.NONE);
    }
  }

  private Action createCloseAction() {
    Action close = new Action(Messages.getString("action.close")) {
      @Override
      public void run() {
        ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] { console });
      }
    };
    close.setToolTipText(Messages.getString("action.close"));
    close.setImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_REMOVE));
    close.setHoverImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_REMOVE));
    close.setDisabledImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_REMOVE_DISABLED));
    return close;
  }

  private Action createTerminateAction() {
    Action terminate = new Action(Messages.getString("action.stop")) {
      @Override
      public void run() {
        console.getJob().cancel();
        update();
      }
    };
    terminate.setToolTipText(Messages.getString("action.stop"));
    terminate.setImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_STOP));
    terminate.setHoverImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_STOP));
    terminate.setDisabledImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_STOP_DISABLED));
    return terminate;
  }

  private ImageDescriptor getSharedImage(String image) {
    return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(image);
  }

  @Override
  public void activated() {
    // nothing to do
  }

  @Override
  public void deactivated() {
    // nothing to do
  }

  @Override
  public void dispose() {
  }

  @Override
  public <C> C getAdapter(Class<C> required) {
    return null;
  }

}

