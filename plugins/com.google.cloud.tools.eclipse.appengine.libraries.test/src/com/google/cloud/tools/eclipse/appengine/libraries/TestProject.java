package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.junit.Assert.fail;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.rules.ExternalResource;

public final class TestProject extends ExternalResource {

  private IJavaProject javaProject;

  @Override
  protected void before() throws Throwable {
    createJavaProject("test" + Math.random());
  }

  @Override
  protected void after() {
    try {
      javaProject.getProject().delete(true, null);
    } catch (CoreException e) {
      fail("Could not delete project");
    }
  }

  public IJavaProject getJavaProject() {
    return javaProject;
  }

  private void createJavaProject(String projectName) throws CoreException, JavaModelException {
    IProjectDescription newProjectDescription = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
    newProjectDescription.setNatureIds(new String[]{JavaCore.NATURE_ID});
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    project.create(newProjectDescription, null);
    project.open(null);
    javaProject = JavaCore.create(project);
    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    IClasspathEntry[] newRawClasspath = new IClasspathEntry[rawClasspath.length + 1];
    System.arraycopy(rawClasspath, 0, newRawClasspath, 0, rawClasspath.length);
    newRawClasspath[newRawClasspath.length - 1] = JavaCore.newContainerEntry(new Path("aaa/bbb"));
    javaProject.setRawClasspath(newRawClasspath, null);
  }
}