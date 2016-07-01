package com.google.cloud.tools.eclipse.appengine.facets;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import com.google.cloud.tools.eclipse.util.MavenUtils;

public class FacetInstallDelegate implements IDelegate {
  
  @Override
  public void execute(IProject project,
                      IProjectFacetVersion version,
                      Object config,
                      IProgressMonitor monitor) throws CoreException {
    if (!MavenUtils.hasMavenNature(project)) { // Maven handles classpath in maven projects.
      IJavaProject javaProject = JavaCore.create(project);
      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      IClasspathEntry[] newClasspath = new IClasspathEntry[rawClasspath.length + 1];
      System.arraycopy(rawClasspath, 0, newClasspath, 0, rawClasspath.length);
      newClasspath[newClasspath.length - 1] =
          JavaCore.newContainerEntry(new Path(AppEngineSdkClasspathContainer.CONTAINER_ID));
      javaProject.setRawClasspath(newClasspath, monitor);
    }
  }

}
