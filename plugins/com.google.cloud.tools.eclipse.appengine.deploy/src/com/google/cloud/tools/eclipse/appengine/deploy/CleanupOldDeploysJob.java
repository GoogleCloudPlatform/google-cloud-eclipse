package com.google.cloud.tools.eclipse.appengine.deploy;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.google.cloud.tools.eclipse.util.io.DeleteAllVisitor;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;

public class CleanupOldDeploysJob extends Job {

  private static String NAME = Messages.getString("cleanup.deploy.job.name"); //$NON-NLS-1$
  private static int OLD_DIRECTORIES_TO_KEEP = 2;
  private IPath parentTempDir;

  public CleanupOldDeploysJob(IPath parentTempDir) {
    super(NAME);
    this.parentTempDir = parentTempDir;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      SortedSet<File> directories = collectDirectories();
      removeRecentDirectories(directories);
      deleteDirectories(directories);
      return Status.OK_STATUS;
    } catch (IOException e) {
      return StatusUtil.error(this, Messages.getString("cleanup.deploy.job.error"), e); //$NON-NLS-1$
    }
  }

  private SortedSet<File> collectDirectories() throws IOException {
    DirectoryStream<Path> newDirectoryStream = Files.newDirectoryStream(parentTempDir.toFile().toPath());
    SortedSet<File> directories = new TreeSet<>(new ReverseLastModifiedComparator());
    for (Path path : newDirectoryStream) {
      File file = path.toFile();
      if (file.isDirectory()) {
        directories.add(file);
      }
    }
    return directories;
  }

  private void removeRecentDirectories(SortedSet<File> directories) {
    Iterator<File> iterator = directories.iterator();
    for (int i = 0; i < OLD_DIRECTORIES_TO_KEEP && iterator.hasNext(); ++i) {
      iterator.next();
      iterator.remove();
    }
  }

  private void deleteDirectories(SortedSet<File> directories) throws IOException {
    for (File file : directories) {
      Files.walkFileTree(file.toPath(), new DeleteAllVisitor());
    }
  }
  /**
   * Comparator that sorts files on reversed order of last modification, i.e. the file that was modified
   * more recently will be "smaller"
   */
  private final class ReverseLastModifiedComparator implements Comparator<File> {
    @Override
    public int compare(File o1, File o2) {
      return - Long.compare(o1.lastModified(), o2.lastModified());
    }
  }


}
