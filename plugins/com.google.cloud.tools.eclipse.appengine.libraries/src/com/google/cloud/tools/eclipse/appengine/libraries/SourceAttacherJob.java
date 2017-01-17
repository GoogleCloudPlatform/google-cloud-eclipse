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
import org.eclipse.osgi.util.NLS;

class SourceAttacherJob extends Job {
  
  private static final Logger logger = Logger.getLogger(SourceAttacherJob.class.getName());
  
  private final IJavaProject javaProject;
  private final IPath containerPath;
  private final IPath libraryPath;
  private Callable<IPath> sourceArtifactPathProvider;

  SourceAttacherJob(IJavaProject javaProject, IPath containerPath, IPath libraryPath,
                    Callable<IPath> sourceArtifactPathProvider) {
    super(NLS.bind(Messages.SourceAttachmentDownloaderJobName, javaProject.getProject().getName()));
    this.javaProject = javaProject;
    this.containerPath = containerPath;
    this.libraryPath = libraryPath;
    this.sourceArtifactPathProvider = sourceArtifactPathProvider;
    setRule(javaProject.getSchedulingRule());
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      IClasspathContainer container = JavaCore.getClasspathContainer(containerPath, javaProject);
      if (container instanceof LibraryClasspathContainer) {
        LibraryClasspathContainer libraryClasspathContainer = (LibraryClasspathContainer) container;
        IClasspathEntry[] classpathEntries = libraryClasspathContainer.getClasspathEntries();
        IClasspathEntry[] newClasspathEntries = new IClasspathEntry[classpathEntries.length];
        System.arraycopy(classpathEntries, 0, newClasspathEntries, 0, classpathEntries.length);
        for (int i = 0; i < newClasspathEntries.length; i++) {
          IClasspathEntry entry = newClasspathEntries[i];
          if (entry.getPath().equals(libraryPath)) {
            IPath sourceArtifactPath = sourceArtifactPathProvider.call();
            newClasspathEntries[i] = JavaCore.newLibraryEntry(entry.getPath(),
                                                              sourceArtifactPath,
                                                              null /* sourceAttachmentRootPath */,
                                                              entry.getAccessRules(),
                                                              entry.getExtraAttributes(),
                                                              entry.isExported());
          }
        }
        LibraryClasspathContainer newContainer =
            libraryClasspathContainer.copyWithNewEntries(newClasspathEntries);
        JavaCore.setClasspathContainer(containerPath, new IJavaProject[]{ javaProject },
                                       new IClasspathContainer[]{ newContainer }, monitor);
      } else {
        logger.log(Level.FINE, NLS.bind(Messages.ContainerClassUnexpected,
                                        container.getClass().getName(),
                                        LibraryClasspathContainer.class.getName()));
      }
    } catch (Exception ex) {
      // it's not needed to be logged normally
      logger.log(Level.FINE, Messages.SourceAttachmentFailed, ex);
    }
    // even if it fails, we should not display an error to the user
    return Status.OK_STATUS;
  }
}