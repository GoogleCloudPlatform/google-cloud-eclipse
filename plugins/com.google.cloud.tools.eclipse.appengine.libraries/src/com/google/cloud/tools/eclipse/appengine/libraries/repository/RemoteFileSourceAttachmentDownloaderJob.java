/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.util.io.FileDownloader;
import java.io.IOException;
import java.net.URL;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

public class RemoteFileSourceAttachmentDownloaderJob extends AbstractSourceAttachmentDownloaderJob {

  static final String PARAM_DOWNLOAD_FOLDER = "downloadFolder";

  @Inject
  @Named(PARAM_DOWNLOAD_FOLDER)
  private IPath downloadFolder;
  @Inject
  private URL sourceUrl;
  
  @Inject
  public RemoteFileSourceAttachmentDownloaderJob(IJavaProject javaProject) {
    super(javaProject);
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
