/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;
import com.google.cloud.tools.eclipse.appengine.deploy.DeployJob;
import com.google.common.base.Preconditions;

public class DeployConsolePageParticipant implements IConsolePageParticipant {

  private DeployConsole console;
  private Action terminateAction;
  private Action closeAction;

  @Override
  public void init(IPageBookViewPage page, IConsole console) {
    Preconditions.checkArgument(console instanceof DeployConsole,
                                "console should be instance of %s",
                                DeployConsole.class.getName());
    this.console = (DeployConsole) console;

    console.addPropertyChangeListener(new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (event.getProperty().equals(DeployConsole.PROPERTY_JOB)) {
          // keep the order of adding a listener and then calling update() to ensure update 
          // is called regardless of when the job finishes
          addJobChangeListener();
          update();
        }
      }
    });
    IActionBars actionBars = page.getSite().getActionBars();
    configureToolBar(actionBars.getToolBarManager());
    // keep the order of adding a listener and then calling update() to ensure update
    // is called regardless of when the job finishes
    addJobChangeListener();
    update();
  }

  private void configureToolBar(IToolBarManager toolbarManager) {
    terminateAction = createTerminateAction();
    toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminateAction);

    closeAction = createCloseAction();
    toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, closeAction);
  }

  private void addJobChangeListener() {
    DeployJob job = console.getJob();
    if (job != null) {
      job.addJobChangeListener(new JobChangeAdapter() {
        @Override
        public void done(IJobChangeEvent event) {
          update();
        }
      });
    }
  }

  private void update() {
    DeployJob job = console.getJob();
    if (job != null) {
      if (terminateAction != null) {
        terminateAction.setEnabled(job.getState() != Job.NONE);
      }

      if (closeAction != null) {
        closeAction.setEnabled(job.getState() == Job.NONE);
      }
    }
  }

  private Action createCloseAction() {
    Action close = new Action(Messages.getString("action.remove")) {
      @Override
      public void run() {
        ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] { console });
      }
    };
    close.setToolTipText(Messages.getString("action.remove"));
    close.setImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_REMOVE));
    close.setHoverImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_REMOVE));
    close.setDisabledImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_REMOVE_DISABLED));
    return close;
  }

  private Action createTerminateAction() {
    Action terminate = new Action(Messages.getString("action.stop")) {
      @Override
      public void run() {
        DeployJob job = console.getJob();
        if (job != null) {
          job.cancel();
          update();
        }
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

