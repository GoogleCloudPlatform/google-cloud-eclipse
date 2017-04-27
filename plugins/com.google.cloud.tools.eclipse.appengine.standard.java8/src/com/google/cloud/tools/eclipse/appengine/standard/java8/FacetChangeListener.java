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

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent.Type;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.common.project.facet.core.events.IProjectFacetActionEvent;

/**
 * Handle facet install and uninstalls, version changes, and generally anything else, and reflect
 * such changes into the {@code appengine-web.xml}. Also add/remove the {@link AppEngineWebBuilder}
 * to monitor for user changes involving the {@code <runtime>java8</runtime>} element in the
 * {@code appengine-web.xml}.
 */
public class FacetChangeListener implements IFacetedProjectListener {
  private static final Logger logger = Logger.getLogger(FacetChangeListener.class.getName());

  @Override
  public void handleEvent(IFacetedProjectEvent event) {
    if (event.getType() != Type.POST_INSTALL && event.getType() != Type.POST_UNINSTALL
        && event.getType() != Type.POST_VERSION_CHANGE) {
      return;
    }
    IProjectFacetActionEvent action = (IProjectFacetActionEvent) event;
    if (!JavaFacet.FACET.equals(action.getProjectFacet())
        && !AppEngineStandardFacet.FACET.equals(action.getProjectFacet())) {
      return;
    }
    logger.fine("Facet change: " + action.getProjectFacet());
    IFacetedProject project = event.getProject();
    if (!AppEngineStandardFacet.hasFacet(project)) {
      removeAppEngineWebBuilder(project.getProject());
      return;
    }
    addAppEngineWebBuilder(project.getProject());
    IFile descriptor = findDescriptor(project);
    if (descriptor == null) {
      logger.warning(project + ": cannot find appengine-web.xml");
        return;
      }
    if (project.hasProjectFacet(JavaFacet.VERSION_1_8)) {
      AppEngineDescriptorTransform.addJava8Runtime(descriptor);
    } else {
      AppEngineDescriptorTransform.removeJava8Runtime(descriptor);
    }
  }

  /**
   * Add our {@code appengine-web.xml} builder that monitors for changes to the {@code <runtime>}
   * element.
   */
  private void addAppEngineWebBuilder(IProject project) {
    try {
      IProjectDescription projectDescription = project.getDescription();
      ICommand[] commands = projectDescription.getBuildSpec();
      for (int i = 0; i < commands.length; i++) {
        if (AppEngineWebBuilder.BUILDER_ID.equals(commands[i].getBuilderName())) {
          return;
        }
      }
      ICommand[] newCommands = new ICommand[commands.length + 1];
      // Add it after other builders.
      System.arraycopy(commands, 0, newCommands, 0, commands.length);
      // add builder to project
      ICommand command = projectDescription.newCommand();
      command.setBuilderName(AppEngineWebBuilder.BUILDER_ID);
      newCommands[commands.length] = command;
      projectDescription.setBuildSpec(newCommands);
      project.setDescription(projectDescription, null);
      logger.finer(project + ": added AppEngineWebBuilder");
    } catch (CoreException ex) {
      logger.log(Level.SEVERE, "Unable to add builder for " + project, ex);
    }
  }

  /**
   * Remove our {@code appengine-web.xml} builder that monitors for changes to the {@code <runtime>}
   * element.
   */
  private void removeAppEngineWebBuilder(IProject project) {
    try {
      IProjectDescription projectDescription = project.getDescription();
      ICommand[] commands = projectDescription.getBuildSpec();
      for (int i = 0; i < commands.length; i++) {
        if (AppEngineWebBuilder.BUILDER_ID.equals(commands[i].getBuilderName())) {
          ICommand[] newCommands = new ICommand[commands.length - 1];
          System.arraycopy(commands, 0, newCommands, 0, i);
          System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
          projectDescription.setBuildSpec(newCommands);
          project.setDescription(projectDescription, null);
          logger.finer(project + ": removed AppEngineWebBuilder");
          return;
        }
      }
    } catch (CoreException ex) {
      logger.log(Level.SEVERE, "Unable to remove builder for " + project, ex);
    }

  }

  /**
   * Find the <code>appengine-web.xml</code> file.
   * @return the file or {@code null} if not found
   */
  private IFile findDescriptor(IFacetedProject project) {
    IFile descriptor =
        WebProjectUtil.findInWebInf(project.getProject(), new Path("appengine-web.xml"));
    return descriptor;
  }

}
