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

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.ui.internal.ImageResource;
import org.eclipse.wst.server.ui.internal.Messages;

/**
 * Adds a stop button for the App Engine runtime to the {@link LocalAppEngineConsole}
 */
@SuppressWarnings("restriction") // For ImageResource, Messages
public class LocalAppEngineConsolePageParticipant implements IConsolePageParticipant {
  private LocalAppEngineConsole console;
  private Action terminateAction;
  private IServerListener serverStateListener = new IServerListener() {
    @Override
    public void serverChanged(ServerEvent event) {
      update();
    }
  };
  
  @Override
  public <T> T getAdapter(Class<T> required) {
    return null;
  }

  @Override
  public void init(IPageBookViewPage page, IConsole console) {
    this.console = (LocalAppEngineConsole) console;

    // contribute to toolbar
    IActionBars actionBars = page.getSite().getActionBars();
    configureToolBar(actionBars.getToolBarManager());

    LocalAppEngineServerBehaviour serverBehaviour = this.console.getServerBehaviourDelegate();
    if (serverBehaviour != null) {
      serverBehaviour.getServer().addServerListener(serverStateListener);
    }
  }

  @Override
  public void dispose() {
    LocalAppEngineServerBehaviour serverBehaviour = console.getServerBehaviourDelegate();
    if (serverBehaviour != null) {
      serverBehaviour.getServer().removeServerListener(serverStateListener);
    }
    terminateAction = null;
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
    terminateAction = new Action(Messages.actionStop) {
      @Override
      public void run() {
        //code to execute when button is pressed
        LocalAppEngineServerBehaviour serverBehaviour = console.getServerBehaviourDelegate();
        if (serverBehaviour != null) {
          // try to initiate a nice shutdown
          boolean force = serverBehaviour.getServer().getServerState() == IServer.STATE_STOPPING;
          serverBehaviour.stop(force);
        }
        update();
      }
    };
    terminateAction.setToolTipText(Messages.actionStopToolTip);
    terminateAction.setImageDescriptor(ImageResource.getImageDescriptor(ImageResource.IMG_ELCL_STOP));
    terminateAction.setHoverImageDescriptor(ImageResource.getImageDescriptor(ImageResource.IMG_CLCL_STOP));
    terminateAction.setDisabledImageDescriptor(ImageResource.getImageDescriptor(ImageResource.IMG_DLCL_STOP));

    toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminateAction);
  }

  private void update() {
    if (terminateAction != null) {
      LocalAppEngineServerBehaviour serverBehaviour = console.getServerBehaviourDelegate();
      if (serverBehaviour != null) {
        // it's ok for us to call #canStop() since it's our implementation
        IStatus status = serverBehaviour.canStop();
        terminateAction.setEnabled(status.isOK());
      }
    }
  }
 
}
