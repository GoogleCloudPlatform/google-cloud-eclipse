package com.google.cloud.tools.eclipse.appengine.libraries;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

class SourceAttacherJob extends Job {
  
  private static final Logger logger = Logger.getLogger(SourceAttacherJob.class.getName());
  
  private final IJavaProject javaProject;
  private final IPath containerPath;
  private final IPath libraryPath;
  private Callable<IPath> sourceArtifactPathFuture;

  SourceAttacherJob(IJavaProject javaProject, IPath containerPath, IPath libraryPath,
                    Callable<IPath> callable) {
    super("Attaching source");
    this.javaProject = javaProject;
    this.containerPath = containerPath;
    this.libraryPath = libraryPath;
    this.sourceArtifactPathFuture = callable;
    setRule(javaProject.getSchedulingRule());
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      IClasspathContainer container = JavaCore.getClasspathContainer(containerPath, javaProject);
      LibraryClasspathContainer libraryClasspathContainer = (LibraryClasspathContainer) container;
      IClasspathEntry[] classpathEntries = libraryClasspathContainer.getClasspathEntries();
      IClasspathEntry[] newClasspathEntries = new IClasspathEntry[classpathEntries.length];
      System.arraycopy(classpathEntries, 0, newClasspathEntries, 0, classpathEntries.length);
      for (int i = 0; i < newClasspathEntries.length; i++) {
        if (newClasspathEntries[i].getPath().equals(libraryPath)) {
          IPath sourceArtifactPath = sourceArtifactPathFuture.call();
          newClasspathEntries[i] = JavaCore.newLibraryEntry(newClasspathEntries[i].getPath(), sourceArtifactPath, null, newClasspathEntries[i].getAccessRules(), newClasspathEntries[i].getExtraAttributes(), newClasspathEntries[i].isExported());
        }
      }
      LibraryClasspathContainer newContainer = libraryClasspathContainer.copyWithNewEntries(newClasspathEntries);
      JavaCore.setClasspathContainer(containerPath, new IJavaProject[]{ javaProject }, new IClasspathContainer[]{ newContainer }, monitor);
    } catch (Exception ex) {
      // it's not needed to be logged normally
      logger.log(Level.FINE, "Could not attach source", ex);
    }
    // even if it fails, we should not display an error to the user
    return Status.OK_STATUS;
  }
}