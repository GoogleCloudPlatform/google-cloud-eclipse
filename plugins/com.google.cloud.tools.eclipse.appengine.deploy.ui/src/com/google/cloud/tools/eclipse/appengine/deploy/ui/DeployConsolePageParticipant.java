package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
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
  protected boolean closeVisible = false;

  @Override
  public <C> C getAdapter(Class<C> required) {
    return null;
  }

  @Override
  public void init(IPageBookViewPage page, IConsole console) {
    if (console instanceof DeployConsole) {
      this.console = (DeployConsole) console;
    } else {
      logger.log(Level.SEVERE, "console is instance of "
          + console.getClass().getName()
          + ", expected was "
          + DeployConsole.class.getName());
    }
    // contribute to toolbar
    IActionBars actionBars = page.getSite().getActionBars();
    configureToolBar(actionBars.getToolBarManager());
    initInternal();
    update();
  }

  @Override
  public void dispose() {
    terminateAction = null;
    closeAction = null;
  }

  @Override
  public void activated() {
    update();
  }

  @Override
  public void deactivated() {
    update();
  }

  private void configureToolBar(IToolBarManager toolbarManager) {
    terminateAction = new Action(Messages.getString("action.stop")) {
      @Override
      public void run() {
        StandardDeployJob deployJob = findJob();
        if (deployJob != null) {
          deployJob.cancel();
        }
        update();
      }
    };
    terminateAction.setToolTipText(Messages.getString("action.stop"));
    terminateAction.setImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_STOP));
    terminateAction.setHoverImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_STOP));
    terminateAction.setDisabledImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_STOP_DISABLED));
    toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminateAction);

    closeAction = new Action(Messages.getString("action.close")) {
      @Override
      public void run() {
        ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] { console });
      }
    };
    closeAction.setToolTipText(Messages.getString("action.close"));
    closeAction.setImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_REMOVE));
    closeAction.setHoverImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_REMOVE));
    closeAction.setDisabledImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_REMOVE_DISABLED));
    toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, closeAction);
  }

  private ImageDescriptor getSharedImage(String image) {
    return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(image);
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

  private StandardDeployJob findJob() {
    Job[] jobs = Job.getJobManager().find(StandardDeployJob.FAMILY);
    for (Job job : jobs) {
      StandardDeployJob deployJob = (StandardDeployJob) job;
      if (getConsole().getJob().equals(deployJob)) {
        return deployJob;
      }
    }
    return null;
  }

  protected void initInternal() {
    StandardDeployJob deployJob = findJob();
    if (deployJob != null) {
      deployJob.addJobChangeListener(new IJobChangeListener() {

        @Override
        public void sleeping(IJobChangeEvent event) {
          update();
        }

        @Override
        public void scheduled(IJobChangeEvent event) {
          update();
        }

        @Override
        public void running(IJobChangeEvent event) {
          update();
        }

        @Override
        public void done(IJobChangeEvent event) {
          update();
        }

        @Override
        public void awake(IJobChangeEvent event) {
          update();
        }

        @Override
        public void aboutToRun(IJobChangeEvent event) {
          update();
        }
      });
    }
  }

  private DeployConsole getConsole() {
    return console;
  }

}

