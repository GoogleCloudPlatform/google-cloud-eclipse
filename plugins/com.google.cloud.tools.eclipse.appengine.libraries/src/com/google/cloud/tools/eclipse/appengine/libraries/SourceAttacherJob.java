/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.libraries;

import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
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

/**
 * Job to fill in the source attachment path attribute of an {@link IClasspathEntry}.
 * <p>
 * The {@link IPath} referencing the source artifact is provided by a {@link Callable} object. The
 * job will create a new {@link IClasspathEntry} by copying the original and adding the source
 * attachment path. The {@link LibraryClasspathContainer} associated with the container path will
 * also be replaced with a copy that is identical to the original except for the updated
 * {@link IClasspathEntry}s.
 * <p>
 * If the source resolution or setting the source attachment attribute fails, the job will still
 * return {@link Status#OK_STATUS} as this is not considered an error that the user should be
 * notified of.
 */
public class SourceAttacherJob extends Job {

  private static final Logger logger = Logger.getLogger(SourceAttacherJob.class.getName());

  private final IJavaProject javaProject;
  private final IPath containerPath;
  private final IPath libraryPath;
  private final Callable<IPath> sourceArtifactPathProvider;
  private final LibraryClasspathContainerSerializer serializer;

  public SourceAttacherJob(IJavaProject javaProject, IPath containerPath, IPath libraryPath,
                    Callable<IPath> sourceArtifactPathProvider) {
    super(Messages.getString("SourceAttachmentDownloaderJobName",
                             javaProject.getProject().getName()));
    this.javaProject = javaProject;
    this.containerPath = containerPath;
    this.libraryPath = libraryPath;
    this.sourceArtifactPathProvider = sourceArtifactPathProvider;
    serializer = new LibraryClasspathContainerSerializer();
    setRule(javaProject.getSchedulingRule());
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      IClasspathContainer container = JavaCore.getClasspathContainer(containerPath, javaProject);
      LibraryClasspathContainer newContainer = attachSource(container);

      if (newContainer != null) {
        JavaCore.setClasspathContainer(containerPath, new IJavaProject[]{ javaProject },
            new IClasspathContainer[]{ newContainer }, monitor);
        serializer.saveContainer(javaProject, newContainer);
      }
    } catch (Exception ex) {
      // it's not needed to be logged normally
      logger.log(Level.FINE, Messages.getString("SourceAttachmentFailed"), ex);
    }
    return Status.OK_STATUS;  // even if it fails, we should not display an error to the user
  }

  @VisibleForTesting
  LibraryClasspathContainer attachSource(IClasspathContainer container) throws Exception {
    if (!(container instanceof LibraryClasspathContainer)) {
      logger.log(Level.FINE, Messages.getString("ContainerClassUnexpected",
          container.getClass().getName(), LibraryClasspathContainer.class.getName()));
      return null;
    };

    LibraryClasspathContainer libraryClasspathContainer = (LibraryClasspathContainer) container;
    IPath sourceArtifactPath = sourceArtifactPathProvider.call();
    List<IClasspathEntry> newClasspathEntries = new ArrayList<>();

    for (IClasspathEntry entry : libraryClasspathContainer.getClasspathEntries()) {
      if (!entry.getPath().equals(libraryPath)) {
        newClasspathEntries.add(entry);
      } else {
        newClasspathEntries.add(JavaCore.newLibraryEntry(
            entry.getPath(), sourceArtifactPath, null /* sourceAttachmentRootPath */,
            entry.getAccessRules(), entry.getExtraAttributes(), entry.isExported()));
      }
    }

    return libraryClasspathContainer.copyWithNewEntries(newClasspathEntries);
  }
}