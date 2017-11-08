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

package com.google.cloud.tools.eclipse.appengine.libraries.persistence;

import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainer;
import com.google.cloud.tools.eclipse.appengine.libraries.Messages;
import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Represents a {@link LibraryClasspathContainer} in such a way that it can be easily transformed
 * into JSON.
 */
class SerializableLibraryClasspathContainer {

  private final String description;
  private final String path;
  private final List<SerializableClasspathEntry> entries = new ArrayList<>();
  private List<LibraryFile> libraryFiles;

  SerializableLibraryClasspathContainer(LibraryClasspathContainer container,
      IPath baseDirectory, IPath sourceBaseDirectory) {
    description = container.getDescription();
    path = container.getPath().toString();

    for (IClasspathEntry entry : container.getClasspathEntries()) {
      entries.add(new SerializableClasspathEntry(entry, baseDirectory, sourceBaseDirectory));
    }
    
    libraryFiles = new ArrayList<>(container.getLibraryFiles());
  }

  LibraryClasspathContainer toLibraryClasspathContainer(IJavaProject javaProject, IPath baseDirectory,
      IPath sourceBaseDirectory) {
    List<IClasspathEntry> classpathEntries = new ArrayList<>();

    for (SerializableClasspathEntry entry : entries) {
      classpathEntries.add(entry.toClasspathEntry(baseDirectory, sourceBaseDirectory));
    }
    
    Library masterLibrary = new Library(CloudLibraries.MASTER_CONTAINER_ID);
    masterLibrary.setName(Messages.getString("google.cloud.platform.libraries")); //$NON-NLS-1$
    if (libraryFiles == null) { // we deserialized an old version
      libraryFiles = new ArrayList<>();
    }
    masterLibrary.setLibraryFiles(libraryFiles);

    return new LibraryClasspathContainer(new Path(path), description, classpathEntries,
        libraryFiles);
  }

}
