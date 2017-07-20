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
import com.google.cloud.tools.eclipse.appengine.facets.convert.AppEngineStandardProjectConvertJob;
import com.google.cloud.tools.eclipse.sdk.ui.preferences.CloudSdkPrompter;
import com.google.cloud.tools.eclipse.ui.util.ProjectFromSelectionHelper;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class AppEngineStandardProjectConvertCommandHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = HandlerUtil.getActiveShellChecked(event);
    try {
      IProject project = ProjectFromSelectionHelper.getFirstProject(event);
      if (project == null) {
        throw new NullPointerException("Convert menu enabled for non-project resources");
      }

      // prompt user if Cloud SDK is not configured
      if (CloudSdkPrompter.getCloudSdkLocation(shell) == null) {
        // no further action required: user chose to not configure
        return null;
      }

      IFacetedProject facetedProject = ProjectFacetsManager.create(project,
          true /* convert to faceted project if necessary */, null /* no monitor here */);
      if (AppEngineStandardFacet.hasFacet(facetedProject)) {
        throw new IllegalStateException("Convert menu enabled for App Engine projects");
      }

      AppEngineStandardProjectConvertJob job =
          new AppEngineStandardProjectConvertJob(facetedProject);
      job.setUser(true);
      job.schedule();
    } catch (CoreException ex) {
      ErrorDialog.openError(shell, "Conversion Failed", "Failed to convert to a faceted project",
          ex.getStatus());
    }
    return null; // Must return null per method Javadoc.
  }
}
