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
import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineConfigurationUtil;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.util.MavenUtils;
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
import org.xml.sax.SAXException;

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
        AppEngineConfigurationUtil.findConfigurationFile(
            project.getProject(), new Path("appengine-web.xml"));
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
      boolean hasAppEngineJava8Facet =
          project.hasProjectFacet(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);
      // if not the same, then we update the facet to match the appengine-web.xml
      if (hasAppEngineJava8Facet != hasJava8Runtime) {
        // See https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1941
        // We don't change the Java Facet for Maven builds as the compiler settings are
        // controlled by settings in the pom.xml, and setting compiler settings is difficult to
        // manage in a consistent way
        // (https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-source-and-target.html)
        boolean isMaven = MavenUtils.hasMavenNature(project.getProject());
        if (hasJava8Runtime) {
          setupForJava8Runtime(project, isMaven, monitor);
        } else {
          setupForJava7Runtime(project, isMaven, monitor);
        }
      }
    } catch (SAXException | AppEngineException ex) {
      // Parsing failed due to malformed XML; just don't check the value now.
    } catch (CoreException | IOException ex) {
      logger.log(Level.SEVERE, getProject() + ": error updating facets", ex);
    }
  }

  /**
   * Apply version changes to downgrade to AES with JRE7, Java 1.7, and DWP 2.5.
   */
  private void setupForJava7Runtime(IFacetedProject project, boolean isMaven,
      IProgressMonitor monitor) throws CoreException {
    Set<Action> updates = new HashSet<>();
    updates.add(new Action(Action.Type.VERSION_CHANGE, AppEngineStandardFacet.JRE7, null));
    if (!isMaven) {
      updates.add(new Action(Action.Type.VERSION_CHANGE, JavaFacet.VERSION_1_7, null));
      updates.add(new Action(Action.Type.VERSION_CHANGE, WebFacetUtils.WEB_25, null));
    }
    logger.fine(getProject() + ": changing facets: " + updates);
    project.modify(updates, monitor);

  }

  /**
   * Apply version changes to downgrade to AES with JRE8, Java 1.8, and DWP 3.1.
   */
  private void setupForJava8Runtime(IFacetedProject project, boolean isMaven,
      IProgressMonitor monitor) throws CoreException {
    Set<Action> updates = new HashSet<>();
    updates.add(new Action(Action.Type.VERSION_CHANGE,
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8, null));
    if (!isMaven) {
      updates.add(new Action(Action.Type.VERSION_CHANGE, JavaFacet.VERSION_1_8, null));
      updates.add(new Action(Action.Type.VERSION_CHANGE, WebFacetUtils.WEB_31, null));
    }
    logger.fine(getProject() + ": changing facets: " + updates);
    project.modify(updates, monitor);
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    // nothing to do
  }

}
