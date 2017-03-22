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

package com.google.cloud.tools.eclipse.appengine.facets;

import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexDeployPreferences;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import com.google.cloud.tools.eclipse.util.templates.appengine.AppEngineTemplateUtility;
import java.io.ByteArrayInputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

public class FlexFacetInstallDelegate extends AppEngineFacetInstallDelegate {
  @Override
  public void execute(IProject project,
                      IProjectFacetVersion version,
                      Object config,
                      IProgressMonitor monitor) throws CoreException {
    super.execute(project, version, config, monitor);
    createConfigFiles(project, monitor);
  }

  // TODO: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1640
  // TODO: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1642
  private void createConfigFiles(IProject project, IProgressMonitor monitor) throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 10);

    FlexDeployPreferences flexDeployPreferences = new FlexDeployPreferences(project);
    String appYamlParentPath = flexDeployPreferences.getAppEngineDirectory();
    IFolder appYamlParentFolder = project.getFolder(appYamlParentPath);
    IFile appYaml = appYamlParentFolder.getFile("app.yaml");
    if (appYaml.exists()) {
      return;
    }

    ResourceUtils.createFolders(appYamlParentFolder, progress.newChild(2));
    appYaml.create(new ByteArrayInputStream(new byte[0]), true, progress.newChild(2));
    String appYamlLocation = appYaml.getLocation().toString();
    AppEngineTemplateUtility.copyFileContent(appYamlLocation, AppEngineTemplateUtility.APP_YAML);
    appYaml.refreshLocal(IResource.DEPTH_ZERO, monitor);
  }
}
