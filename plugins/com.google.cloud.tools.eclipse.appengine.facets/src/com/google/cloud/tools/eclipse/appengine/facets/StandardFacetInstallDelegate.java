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

import com.google.cloud.tools.eclipse.util.templates.appengine.AppEngineTemplateUtility;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import java.io.ByteArrayInputStream;
import java.util.Collections;

public class StandardFacetInstallDelegate extends AppEngineFacetInstallDelegate {
  private final static String APPENGINE_WEB_XML = "appengine-web.xml";
  // TODO Change directory for dynamic web module.
  // Differentiate between project with web facets vs 'true' dynamic web modules?
  private final static String APPENGINE_WEB_XML_DIR = "src/main/webapp/WEB-INF/";
  private final static String APPENGINE_WEB_XML_PATH = APPENGINE_WEB_XML_DIR + APPENGINE_WEB_XML;

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
    IFile appEngineWebXml = project.getFile(APPENGINE_WEB_XML_PATH);
    if (appEngineWebXml.exists()) {
      return;
    }

    IFolder configDir = project.getFolder(APPENGINE_WEB_XML_DIR);
    if (!configDir.exists()) {
      Path configDirPath = new Path(APPENGINE_WEB_XML_DIR);
      IContainer current = project;
      for (int i = 0; i < configDirPath.segmentCount(); i++) {
        final String segment = configDirPath.segment( i );
        IFolder folder = current.getFolder(new Path(segment));

        if (!folder.exists()) {
          folder.create( true, true, monitor );
        }
        current = folder;
      }
      configDir = (IFolder) current;
    }

    appEngineWebXml.create(new ByteArrayInputStream(new byte[0]), true, monitor);
    String configFileLocation = appEngineWebXml.getLocation().toString();
    AppEngineTemplateUtility.createFileContent(
        configFileLocation, AppEngineTemplateUtility.APPENGINE_WEB_XML_TEMPLATE, Collections.<String, String> emptyMap());
  }

}
