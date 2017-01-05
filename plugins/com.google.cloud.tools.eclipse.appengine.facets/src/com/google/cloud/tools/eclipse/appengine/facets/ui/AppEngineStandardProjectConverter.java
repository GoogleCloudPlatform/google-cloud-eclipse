/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.facets.ui;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.Messages;
import com.google.cloud.tools.eclipse.ui.util.ProjectFromSelectionHelper;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class AppEngineStandardProjectConverter extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    try {
      IProject project = ProjectFromSelectionHelper.getProject(event);
      if (project == null) {
        throw new NullPointerException("Deploy menu enabled for non-project resources");
      }

      IFacetedProject facetedProject = ProjectFacetsManager.create(project,
          true /* convert to faceted project if necessary */, null /* no monitor here */);
      if (AppEngineStandardFacet.hasAppEngineFacet(facetedProject)) {
        throw new IllegalStateException("Deploy menu enabled for non-project resources");
      }

      if (checkFacetCompatibilty(facetedProject, event)) {
        // TODO(chanseok): This installs (among many others) web.xml and appengine-web.xml into
        // "src/main/webapp/WEB-INF". Check if "WEB-INF" already exists and install them there.
        AppEngineStandardFacet.installAppEngineFacet(facetedProject,
            true /* install Java and facets too (safe even if already installed) */,
            null /* monitor */);
        MessageDialog.openInformation(HandlerUtil.getActiveShell(event),
            Messages.getString("appengine.standard.conversion.success.title"),
            Messages.getString("appengine.standard.conversion.success.message"));
      }

      return null;
    } catch (CoreException ex) {
      throw new ExecutionException("Failed to convert to a faceted project", ex);
    }
  }

  private boolean checkFacetCompatibilty(IFacetedProject project, ExecutionEvent event) {
    if (project.hasProjectFacet(JavaFacet.FACET)) {
      if (!project.hasProjectFacet(JavaFacet.VERSION_1_7)) {
        String required = JavaFacet.VERSION_1_7.getVersionString();
        String installed = project.getInstalledVersion(JavaFacet.FACET).getVersionString();

        MessageDialog.openInformation(HandlerUtil.getActiveShell(event),
            Messages.getString("java.facet.incompatible.title"),
            Messages.getString("java.facet.incompatible.message", required, installed));
        return false;
      }
    }

    if (project.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
      if (!project.hasProjectFacet(WebFacetUtils.WEB_25)) {
        String required = WebFacetUtils.WEB_25.getVersionString();
        String installed = project.getInstalledVersion(WebFacetUtils.WEB_FACET).getVersionString();

        MessageDialog.openInformation(HandlerUtil.getActiveShell(event),
            Messages.getString("web.facet.incompatible.title"),
            Messages.getString("web.facet.incompatible.message", required, installed));
        return false;
      }
    }

    return true;
  }
}
