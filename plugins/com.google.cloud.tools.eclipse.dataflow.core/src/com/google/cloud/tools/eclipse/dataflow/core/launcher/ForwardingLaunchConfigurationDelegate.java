/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;

/**
 * An {@link ILaunchConfigurationDelegate2} that forwards all its method calls to another
 * {@code ILaunchConfigurationDelegate2}. Subclasses should override one or more methods to modify
 * the behavior of the backing Launch Configuration Delegate as desired.
 */
public abstract class ForwardingLaunchConfigurationDelegate implements
    ILaunchConfigurationDelegate2 {

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    delegate().launch(configuration, mode, launch, monitor);
  }

  @Override
  public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
    return delegate().getLaunch(configuration, mode);
  }

  @Override
  public boolean buildForLaunch(ILaunchConfiguration configuration, String mode,
      IProgressMonitor monitor) throws CoreException {
    return delegate().buildForLaunch(configuration, mode, monitor);
  }

  @Override
  public boolean finalLaunchCheck(ILaunchConfiguration configuration, String mode,
      IProgressMonitor monitor) throws CoreException {
    return delegate().finalLaunchCheck(configuration, mode, monitor);
  }

  @Override
  public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode,
      IProgressMonitor monitor) throws CoreException {
    return delegate().preLaunchCheck(configuration, mode, monitor);
  }

  protected abstract ILaunchConfigurationDelegate2 delegate();
}
