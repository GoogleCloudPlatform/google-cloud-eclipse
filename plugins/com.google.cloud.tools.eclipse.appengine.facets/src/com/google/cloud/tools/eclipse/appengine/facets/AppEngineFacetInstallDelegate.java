/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.facets;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import com.google.cloud.tools.appengine.cloudsdk.AppEngineJavaComponentsNotInstalledException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;

public abstract class AppEngineFacetInstallDelegate implements IDelegate   {
  @Override
  public void execute(IProject project,
      IProjectFacetVersion version,
      Object config,
      IProgressMonitor monitor) throws CoreException {
    validateAppEngineJavaComponents();
  }

  private void validateAppEngineJavaComponents() throws CoreException {
    CloudSdk cloudSdk = new CloudSdk.Builder().build();
    try {
      cloudSdk.validateAppEngineJavaComponents();
    } catch (AppEngineJavaComponentsNotInstalledException ex) {
      // to properly display error in message box
      throw new CoreException(StatusUtil.error(getClass(),
          Messages.getString("appengine.java.component.missing"), ex));
    }
  }
}
