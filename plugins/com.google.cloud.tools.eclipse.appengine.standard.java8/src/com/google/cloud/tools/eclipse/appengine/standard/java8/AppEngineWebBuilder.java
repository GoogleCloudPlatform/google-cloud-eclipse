/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.standard.java8;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Builder to monitor user changes relating to the {@code <runtime>java8</runtime>} element in
 * {@code appengine-web.xml}, and to reflect such changes into the project facets.
 */
public class AppEngineWebBuilder extends IncrementalProjectBuilder {
  private static final Logger logger = Logger.getLogger(AppEngineWebBuilder.class.getName());
  static final String BUILDER_ID =
      "com.google.cloud.tools.eclipse.appengine.standard.java8.appengineWeb";

  @Override
  protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
      throws CoreException {
    IFacetedProject project = ProjectFacetsManager.create(getProject());
    if (project == null || !AppEngineStandardFacet.hasFacet(project)) {
      logger.fine(getProject() + ": no build required: no App Engine Standard facet");
      return null;
    }
    IFile appEngineWebDescriptor =
        WebProjectUtil.findInWebInf(project.getProject(), new Path("appengine-web.xml"));
    if (appEngineWebDescriptor == null || !appEngineWebDescriptor.exists()) {
      logger.warning(getProject() + ": no build required: missing appengine-web.xml");
      return null;
    }
    if (kind == FULL_BUILD) {
      checkRuntimeElement(project, appEngineWebDescriptor, monitor);
    } else {
      IResourceDelta delta = getDelta(getProject());
      if (delta == null
          || delta.findMember(appEngineWebDescriptor.getProjectRelativePath()) != null) {
        checkRuntimeElement(project, appEngineWebDescriptor, monitor);
      } else {
        logger.finer(getProject() + ": no build required: appengine-web.xml not changed");
      }
    }
    return null;
  }

  private void checkRuntimeElement(IFacetedProject project, IFile appEngineWebDescriptor,
      IProgressMonitor monitor) {
    try (InputStream input = appEngineWebDescriptor.getContents()) {
      boolean hasJava8Runtime = AppEngineDescriptor.parse(input).isJava8();
      boolean hasJava8Facet = project.hasProjectFacet(JavaFacet.VERSION_1_8);
      // if not the same, then we update the facet to match the appengine-web.xml
      if (hasJava8Facet != hasJava8Runtime) {
        Set<Action> updates = new HashSet<Action>();
        // Can upgrade jst.web to 3.1, but cannot downgrade from 3.1
        if (hasJava8Runtime) {
          updates.add(new Action(Action.Type.VERSION_CHANGE, JavaFacet.VERSION_1_8, null));
          updates.add(new Action(Action.Type.VERSION_CHANGE, WebFacetUtils.WEB_31, null));
        } else {
          updates.add(new Action(Action.Type.VERSION_CHANGE, JavaFacet.VERSION_1_7, null));
        }
        logger.fine(getProject() + ": changing facets: " + updates);
        project.modify(updates, monitor);
      }
    } catch (CoreException | IOException ex) {
      logger.log(Level.SEVERE, getProject() + ": error updating facets", ex);
    }
  }

  protected void clean(IProgressMonitor monitor) throws CoreException {
    // nothing to do
  }

}
