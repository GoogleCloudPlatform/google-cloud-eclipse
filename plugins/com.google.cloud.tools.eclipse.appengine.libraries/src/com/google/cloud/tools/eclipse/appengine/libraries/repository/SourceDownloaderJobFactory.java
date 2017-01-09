package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.util.PathUtil;
import com.google.cloud.tools.eclipse.util.jobs.JobUtil;
import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jdt.core.IJavaProject;

public class SourceDownloaderJobFactory {

  private MavenHelper mavenHelper;
  
  public Job createSourceDownloaderJob(IJavaProject javaProject, MavenCoordinates mavenCoordinates,
                                       IPath classpathEntryPath, URL sourceUrl) {
    if (sourceUrl == null) {
      return createM2SourceDownloaderJob(javaProject, mavenCoordinates, classpathEntryPath);
    } else {
      return createRemoteFileSourceDownloaderJob(javaProject, classpathEntryPath, 
                                                 mavenCoordinates, sourceUrl);
    }
  }
  
  private Job createM2SourceDownloaderJob(final IJavaProject javaProject,
                                          final MavenCoordinates mavenCoordinates,
                                          final IPath classpathEntryPath) {
    return JobUtil.createJob(M2SourceAttachmentDownloaderJob.class, 
                             new JobUtil.ContextParameterSupplier() {
                               @Override
                               public void setParameters(IEclipseContext context) {
                                 context.set(IJavaProject.class, javaProject);
                                 context.set(MavenCoordinates.class, mavenCoordinates);
                                 context.set("classpathEntryPath", classpathEntryPath);
                                 context.set(MavenHelper.class, mavenHelper);
                               }
                             });
  }

  private Job createRemoteFileSourceDownloaderJob(final IJavaProject javaProject,
                                                  final IPath classpathEntryPath,
                                                  final MavenCoordinates mavenCoordinates,
                                                  final URL sourceUrl) {
    return JobUtil.createJob(RemoteFileSourceAttachmentDownloaderJob.class,
                             new JobUtil.ContextParameterSupplier() {
                               @Override
                               public void setParameters(IEclipseContext context) {
                                 context.set(IJavaProject.class, javaProject);
                                 context.set("classpathEntryPath", classpathEntryPath);
                                 context.set("downloadFolder", 
                                     PathUtil.bundleStateBasedMavenFolder(mavenCoordinates));
                                 context.set(URL.class, sourceUrl);
                               }
                             });
  }
}
