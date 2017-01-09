package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.Messages;
import com.google.cloud.tools.eclipse.util.io.FileDownloader;
import java.io.IOException;
import java.net.URL;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

public class RemoteFileSourceAttachmentDownloaderJob extends AbstractSourceAttachmentDownloaderJob {

  @Inject
  @Named("downloadFolder")
  private IPath downloadFolder;
  @Inject
  private URL sourceUrl;
  
  @Inject
  public RemoteFileSourceAttachmentDownloaderJob(IJavaProject javaProject) {
    super(Messages.RemoteFileSourceAttachmentDownloaderJobName, javaProject);
  }

  @Override
  protected IPath getSourcePath(IProgressMonitor monitor) {
    try {
      IPath path = new FileDownloader(downloadFolder).download(sourceUrl, monitor);
      return path;
    } catch (IOException e) {
      // source file is failed to download, this is not an error
      return null;
    }
  }

}
