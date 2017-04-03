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
import com.google.cloud.tools.eclipse.appengine.facets.convert.AppEngineStandardProjectConvertJob;
import com.google.cloud.tools.eclipse.ui.util.ProjectFromSelectionHelper;
import com.google.common.annotations.VisibleForTesting;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class AppEngineStandardProjectConvertCommandHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    try {
      IProject project = ProjectFromSelectionHelper.getProject(event);
      if (project == null) {
        throw new NullPointerException("Convert menu enabled for non-project resources");
      }

      IFacetedProject facetedProject = ProjectFacetsManager.create(project,
          true /* convert to faceted project if necessary */, null /* no monitor here */);
      if (AppEngineStandardFacet.hasFacet(facetedProject)) {
        throw new IllegalStateException("Convert menu enabled for App Engine projects");
      }

      Shell shell = HandlerUtil.getActiveShell(event);
      if (checkFacetCompatibility(facetedProject, new MessageDialogWrapper(shell))) {
        new AppEngineStandardProjectConvertJob(facetedProject).schedule();
      }
      return null;  // Must return null per method Javadoc.
    } catch (CoreException ex) {
      throw new ExecutionException("Failed to convert to a faceted project", ex);
    }
  }

  @VisibleForTesting
  boolean checkFacetCompatibility(IFacetedProject facetedProject,
      MessageDialogWrapper dialogWrapper) {
    if (facetedProject.hasProjectFacet(JavaFacet.FACET)) {
      if (!facetedProject.hasProjectFacet(JavaFacet.VERSION_1_7)) {
        String required = JavaFacet.VERSION_1_7.getVersionString();
        String installed = facetedProject.getInstalledVersion(JavaFacet.FACET).getVersionString();

        dialogWrapper.openInformation(
            Messages.getString("java.facet.incompatible.title"),
            Messages.getString("java.facet.incompatible.message",
                               facetedProject.getProject().getName(), required, installed));
        return false;
      }
    }

    if (facetedProject.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
      if (!facetedProject.hasProjectFacet(WebFacetUtils.WEB_25)) {
        String required = WebFacetUtils.WEB_25.getVersionString();
        String installed =
            facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET).getVersionString();

        dialogWrapper.openInformation(
            Messages.getString("web.facet.incompatible.title"),
            Messages.getString("web.facet.incompatible.message",
                               facetedProject.getProject().getName(), required, installed));
        return false;
      }
    }

    return true;
  }

  /**
   * Wraps the static method {@link MessageDialog#openInformation} for unit testing.
   */
  @VisibleForTesting
  public static class MessageDialogWrapper {

    private Shell shell;

    public MessageDialogWrapper(Shell shell) {
      this.shell = shell;
    }

    public void openInformation(String title, String message) {
      MessageDialog.openInformation(shell, title, message);
    }
  }
}
