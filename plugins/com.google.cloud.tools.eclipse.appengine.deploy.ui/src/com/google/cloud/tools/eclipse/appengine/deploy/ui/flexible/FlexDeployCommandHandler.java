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

package com.google.cloud.tools.eclipse.appengine.deploy.ui.flexible;

import com.google.cloud.tools.eclipse.appengine.deploy.DeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.StagingDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexDeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexMavenPackagedProjectStagingDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexWarStagingDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployCommandHandler;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployPreferencesDialog;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexJarFacet;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexWarFacet;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Preconditions;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class FlexDeployCommandHandler extends DeployCommandHandler {

  public FlexDeployCommandHandler() {
    super(AnalyticsEvents.APP_ENGINE_DEPLOY_FLEXIBLE);
  }

  @Override
  protected DeployPreferencesDialog newDeployPreferencesDialog(Shell shell, IProject project) {
    String title = Messages.getString("deploy.preferences.dialog.title.flexible");
    return new FlexDeployPreferencesDialog(shell, title, project);
  }

  @Override
  protected StagingDelegate getStagingDelegate(IProject project) throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    Preconditions.checkNotNull(facetedProject);

    String appYamlPath = new FlexDeployPreferences(project).getAppYamlPath();
    IPath appYaml = resolveFile(appYamlPath, project.getLocation());
    IPath appEngineDirectory = appYaml.removeLastSegments(1);

    if (AppEngineFlexWarFacet.hasFacet(facetedProject)) {
      return new FlexWarStagingDelegate(project, appEngineDirectory);
    } else if (AppEngineFlexJarFacet.hasFacet(facetedProject)) {
      if (MavenUtils.hasMavenNature(project)) {
        return new FlexMavenPackagedProjectStagingDelegate(project, appEngineDirectory);
      } else {
        throw new IllegalStateException("BUG: command enabled for non-Maven flex projects");
      }
    } else {
      throw new IllegalStateException("BUG: command enabled for non-flex projects");
    }
  }

  protected static IPath resolveFile(String path, IPath baseDirectory) throws CoreException {
    IPath fullPath = new Path(path);
    if (!fullPath.isAbsolute()) {
      fullPath = baseDirectory.append(path);
    }

    if (!fullPath.toFile().exists()) {
      throw new CoreException(
          StatusUtil.error(FlexDeployCommandHandler.class, fullPath + " does not exist."));
    }
    return fullPath;
  }

  @Override
  protected DeployPreferences getDeployPreferences(IProject project) {
    return new FlexDeployPreferences(project);
  }
}
