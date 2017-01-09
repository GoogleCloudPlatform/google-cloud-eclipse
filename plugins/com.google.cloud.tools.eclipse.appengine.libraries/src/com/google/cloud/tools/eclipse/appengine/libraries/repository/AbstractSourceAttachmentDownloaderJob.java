package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainer;
import com.google.cloud.tools.eclipse.appengine.libraries.Messages;
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Preconditions;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.util.NLS;

public abstract class AbstractSourceAttachmentDownloaderJob extends Job {

  @Inject
  @Named("classpathEntryPath")
  private IPath classpathEntryPath;
  @Inject
  private LibraryClasspathContainerSerializer serializer;
  @Inject
  private IJavaProject javaProject;

  @Inject
  public AbstractSourceAttachmentDownloaderJob(@Named("jobName") String name,
                                               IJavaProject javaProject) {
    super(name);
    Preconditions.checkNotNull(javaProject, "javaProject is null");
    this.javaProject = javaProject;
    setRule(javaProject.getSchedulingRule());
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      IPath sourcePath = getSourcePath(monitor);
      if (sourcePath != null) {
        setSourceAttachmentPath(sourcePath, monitor);
      }
      return Status.OK_STATUS;
    } catch (IOException | CoreException ex) {
      return StatusUtil.error(this, "Could not attach source path", ex);
    }
  }

  /**
   * Subclasses must implement this method to return an {@link IPath} that points to the
   * <code>jar</code> file containing the sources for the library corresponding to
   * {@link #classpathEntryPath}.
   */
  protected abstract IPath getSourcePath(IProgressMonitor monitor);

  /**
   * Finds the library entry in the project's classpath with path matching 
   * {@link #classpathEntryPath} and sets the source attachment path to <code>sourcePath</code>
   */
  private void setSourceAttachmentPath(IPath sourcePath,
                                       IProgressMonitor monitor) throws IOException, CoreException {
    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    for (int i = 0; i < rawClasspath.length; i++) {
      LibraryClasspathContainer libraryContainer = getLibraryClasspathContainer(rawClasspath[i]);
      if (libraryContainer != null) {
        IClasspathEntry[] classpathEntries = libraryContainer.getClasspathEntries();
        for (int j = 0; j < classpathEntries.length; j++) {
          if (classpathEntries[j].getPath().equals(classpathEntryPath)) {
            updateClasspathEntrySourcePath(classpathEntries, j, sourcePath);
            LibraryClasspathContainer newLibraryContainer =
                copyContainerWithNewEntries(libraryContainer, classpathEntries);
            JavaCore.setClasspathContainer(libraryContainer.getPath(),
                                           new IJavaProject[]{ javaProject },
                                           new IClasspathContainer[]{ newLibraryContainer },
                                           monitor);
            serializer.saveContainer(javaProject, newLibraryContainer);
            return;
          }
        }
      }
    }
    throw new CoreException(StatusUtil.error(this, NLS.bind(Messages.SetSourceAttachmentFailed,
                                                            classpathEntryPath.toOSString(),
                                                            javaProject.getProject().getName())));
  }

  /**
   * Creates a new {@link LibraryClasspathContainer} with the same path and description as
   * <code>libraryContainer</code>'s but with the <code>classpathEntries</code>.
   * @param libraryContainer the original container whose path and description are copied to the
   * result
   * @param classpathEntries the classpath entries of the new container
   */
  private LibraryClasspathContainer copyContainerWithNewEntries(
      LibraryClasspathContainer libraryContainer, IClasspathEntry[] classpathEntries) {
    LibraryClasspathContainer newLibraryContainer =
        new LibraryClasspathContainer(libraryContainer.getPath(),
                                      libraryContainer.getDescription(),
                                      classpathEntries);
    return newLibraryContainer;
  }

  /**
   * Updates a classpath entry by adding <code>sourcePath</code> as source attachment path.
   * @param entryIndex the index of the element in the array that will be updated
   */
  private void updateClasspathEntrySourcePath(IClasspathEntry[] entries, int entryIndex,
      IPath sourcePath) {
    entries[entryIndex] = JavaCore.newLibraryEntry(entries[entryIndex].getPath(),
                                                   sourcePath,
                                                   null,
                                                   entries[entryIndex].getAccessRules(),
                                                   entries[entryIndex].getExtraAttributes(),
                                                   entries[entryIndex].isExported());
  }

  /**
   * Returns the {@link LibraryClasspathContainer} corresponding to the {@link IClasspathEntry} if
   * one exists, otherwise <code>null</code>.
   */
  private LibraryClasspathContainer getLibraryClasspathContainer(IClasspathEntry entry)
                                                                        throws JavaModelException {
    if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
      IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), javaProject);
      if (container instanceof LibraryClasspathContainer) {
        return (LibraryClasspathContainer) container;
      }
    }
    return null;
  }

  protected IJavaProject getJavaProject() {
    return javaProject;
  }
}