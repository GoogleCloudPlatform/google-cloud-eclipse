
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
 * Handle facet install and uninstalls, version changes, and generally anything else, and add/remove
 * the our {@code appengine-web.xml} builder and its {@code <runtime>java8</runtime>} element.
 */
public class FacetChangeListener implements IFacetedProjectListener {
  private static final Logger logger = Logger.getLogger(FacetChangeListener.class.getName());

  @Override
  public void handleEvent(IFacetedProjectEvent event) {
    IFacetedProject project = event.getProject();
    if (!AppEngineStandardFacet.hasFacet(project)) {
      removeAppEngineWebBuilder(project.getProject());
      return;
    }
    addAppEngineWebBuilder(project.getProject());
    if (event.getType() == Type.POST_INSTALL || event.getType() == Type.POST_VERSION_CHANGE) {
      IProjectFacetActionEvent action = (IProjectFacetActionEvent) event;
      if (JavaFacet.FACET.equals(action.getProjectFacet())) {
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
    }
  }

  /**
   * Add our {@code appengine-web.xml} builder that monitors for changes to the {@code <runtime>}
   * element.
   */
  private void addAppEngineWebBuilder(IProject project) {
    try {
      IProjectDescription desc = project.getDescription();
      ICommand[] commands = desc.getBuildSpec();
      for (int i = 0; i < commands.length; i++) {
        if (AppEngineWebBuilder.BUILDER_ID.equals(commands[i].getBuilderName())) {
          return;
        }
      }
      ICommand[] nc = new ICommand[commands.length + 1];
      // Add it after other builders.
      System.arraycopy(commands, 0, nc, 0, commands.length);
      // add builder to project
      ICommand command = desc.newCommand();
      command.setBuilderName(AppEngineWebBuilder.BUILDER_ID);
      nc[commands.length] = command;
      desc.setBuildSpec(nc);
      project.setDescription(desc, null);
      logger.finer(project + ": added AppEngineWebBuilder");
    } catch (CoreException ex) {
      logger.log(Level.SEVERE, "Unable to add builder for " + project, ex);
    }
  }

  /**
   * Add our {@code appengine-web.xml} builder that monitors for changes to the {@code <runtime>}
   * element.
   */
  private void removeAppEngineWebBuilder(IProject project) {
    try {
      IProjectDescription desc = project.getDescription();
      ICommand[] commands = desc.getBuildSpec();
      for (int i = 0; i < commands.length; i++) {
        if (AppEngineWebBuilder.BUILDER_ID.equals(commands[i].getBuilderName())) {
          ICommand[] nc = new ICommand[commands.length - 1];
          System.arraycopy(commands, 0, nc, 0, i);
          System.arraycopy(commands, i + 1, nc, i, commands.length - i - 1);
          desc.setBuildSpec(nc);
          project.setDescription(desc, null);
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
   * 
   * @return the file or {@code null} if not found
   */
  private IFile findDescriptor(IFacetedProject project) {
    IFile descriptor =
        WebProjectUtil.findInWebInf(project.getProject(), new Path("appengine-web.xml"));
    return descriptor;
  }

}
