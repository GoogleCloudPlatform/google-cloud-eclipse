package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.Messages;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import javax.inject.Inject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

public class M2SourceAttachmentDownloaderJob extends AbstractSourceAttachmentDownloaderJob {

  @Inject
  protected MavenCoordinates mavenCoordinates;
  @Inject
  private MavenHelper mavenHelper;

  @Inject
  public M2SourceAttachmentDownloaderJob(IJavaProject javaProject) {
    super(Messages.M2SourceAttachmentDownloaderJobName, javaProject);
  }
  
  @Override
  protected IPath getSourcePath(IProgressMonitor monitor) {
    return mavenHelper.getMavenSourceJarLocation(mavenCoordinates, monitor);
  }
}
