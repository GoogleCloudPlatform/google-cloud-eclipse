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

import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import com.google.cloud.tools.eclipse.util.templates.appengine.AppEngineTemplateUtility;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

public class StandardFacetInstallDelegate extends AppEngineFacetInstallDelegate {
  private final static String DEFAULT_WEB_PATH = "src/main/webapp";

  private final static String WEB_INF = "WEB-INF/";
  private final static String APPENGINE_WEB_XML = "appengine-web.xml";

  @Override
  public void execute(IProject project,
                      IProjectFacetVersion version,
                      Object config,
                      IProgressMonitor monitor) throws CoreException {
    super.execute(project, version, config, monitor);
    createConfigFiles(project, monitor);
  }

  /**
   * Creates an appengine-web.xml file in the WEB-INF folder if it doesn't exist
   */
  private void createConfigFiles(IProject project, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 10);

    // The virtual component model is very flexible, but we assume that
    // the WEB-INF/appengine-web.xml isn't a virtual file remapped elsewhere
    IFolder webInfDir = getWebInfDirectory(project);
    IFile appEngineWebXml = webInfDir.getFile(APPENGINE_WEB_XML);

    if (appEngineWebXml.exists()) {
      return;
    }

    ResourceUtils.createFolders(webInfDir, progress.newChild(2));

    appEngineWebXml.create(new ByteArrayInputStream(new byte[0]), true, progress.newChild(2));
    String configFileLocation = appEngineWebXml.getLocation().toString();
    AppEngineTemplateUtility.createFileContent(
        configFileLocation, AppEngineTemplateUtility.APPENGINE_WEB_XML_TEMPLATE,
        Collections.<String, String>emptyMap());
    progress.worked(6);
  }

  private IFolder getWebInfDirectory(IProject project) {
    // Try to obtain the directory as if it was a Dynamic Web Project
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component != null && component.exists()) {
      IVirtualFolder root = component.getRootFolder();
      // the root should exist, but the WEB-INF may not yet exist
      if (root.exists()) {
        return (IFolder) root.getFolder(WEB_INF).getUnderlyingFolder();
      }
    }
    // Otherwise it's seemingly fair game
    return project.getFolder(DEFAULT_WEB_PATH).getFolder(WEB_INF);

  }
}
