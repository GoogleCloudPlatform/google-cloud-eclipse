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

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;

/**
 * Allow the Debug Launch framework to terminate our Dev App Server. Required for <em>Run</em>-style
 * launches as otherwise the launch becomes a zombie launch that cannot be terminated nor removed.
 */
public class LocalAppEngineServerDebugTarget extends DebugElement implements IDebugTarget {
  private ILaunch launch;
  private LocalAppEngineServerBehaviour serverBehaviour;

  // Fire a {@link DebugEvent#TERMINATED} event when the server is stopped
  private IServerListener serverEventsListener = new IServerListener() {
    @Override
    public void serverChanged(ServerEvent event) {
      if (event.getState() == IServer.STATE_STOPPED) {
        fireTerminateEvent();
        release();
      }
    }
  };

  // Ensure that our listeners are disconnected on remove
  private ILaunchesListener launchesListener = new ILaunchesListener() {
    @Override
    public void launchesRemoved(ILaunch[] launches) {
      // Ensure our listeners
      for (ILaunch launch : launches) {
        if (launch.equals(getLaunch())) {
          release();
          return;
        }
      }
    }

    @Override
    public void launchesAdded(ILaunch[] launches) {}

    @Override
    public void launchesChanged(ILaunch[] launches) {}
  };

  /**
   * Add a debug target to the specified launch.
   */
  public static void addTarget(ILaunch launch, LocalAppEngineServerBehaviour serverBehaviour) {
    LocalAppEngineServerDebugTarget target = new LocalAppEngineServerDebugTarget(launch, serverBehaviour);
    launch.addDebugTarget(target);
    target.install();
    target.fireCreationEvent();
  }

  private LocalAppEngineServerDebugTarget(ILaunch launch,
      LocalAppEngineServerBehaviour serverBehaviour) {
    super(null);
    this.launch = launch;
    this.serverBehaviour = serverBehaviour;
  }

  /** Install any required listeners. */
  private void install() {
    serverBehaviour.getServer().addServerListener(serverEventsListener);
    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(launchesListener);
  }

  /** Remove any installed listeners. */
  private void release() {
    DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(launchesListener);
    serverBehaviour.getServer().removeServerListener(serverEventsListener);
  }

  @Override
  public String getName() throws DebugException {
    return serverBehaviour.getServer().getName();
  }

  @Override
  public String getModelIdentifier() {
    return "Google Cloud SDK Dev App Server";
  }

  @Override
  public ILaunch getLaunch() {
    return launch;
  }

  @Override
  public IDebugTarget getDebugTarget() {
    return this;
  }

  @Override
  public <T> T getAdapter(Class<T> adapter) {
    // Could be IElementContentProvider, IElementLabelProvider, IDebugModelProvider
    if (adapter.isInstance(serverBehaviour)) {
      return adapter.cast(serverBehaviour);
    }
    return super.getAdapter(adapter);
  }

  @Override
  public boolean canTerminate() {
    return true;
  }

  @Override
  public boolean isTerminated() {
    int state = serverBehaviour.getServer().getServerState();
    return state == IServer.STATE_STOPPED;
  }

  @Override
  public void terminate() throws DebugException {
    int state = serverBehaviour.getServer().getServerState();
    if (state != IServer.STATE_STOPPED) {
      serverBehaviour.stop(state == IServer.STATE_STOPPING);
    }
  }

  @Override
  public boolean canSuspend() {
    return false;
  }

  @Override
  public void suspend() throws DebugException {
    throw notSupportedException();
  }

  @Override
  public boolean isSuspended() {
    return false;
  }

  @Override
  public boolean canResume() {
    return false;
  }

  @Override
  public void resume() throws DebugException {
    throw notSupportedException();
  }

  @Override
  public boolean supportsBreakpoint(IBreakpoint breakpoint) {
    return false;
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {}

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {}

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {}

  @Override
  public boolean canDisconnect() {
    return false;
  }

  @Override
  public boolean isDisconnected() {
    return false;
  }

  @Override
  public void disconnect() throws DebugException {
    throw notSupportedException();
  }

  @Override
  public boolean supportsStorageRetrieval() {
    return false;
  }

  @Override
  public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
    throw notSupportedException();
  }

  @Override
  public boolean hasThreads() throws DebugException {
    return false;
  }

  @Override
  public IThread[] getThreads() throws DebugException {
    throw notSupportedException();
  }

  @Override
  public IProcess getProcess() {
    return null;
  }

  private DebugException notSupportedException() {
    return new DebugException(new Status(IStatus.ERROR,
        "com.google.cloud.tools.eclipse.appengine.localserver", "Operation not supported"));
  }
}
