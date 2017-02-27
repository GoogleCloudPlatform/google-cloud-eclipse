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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Represents a {@link LibraryClasspathContainer} in such a way that it can be easily transformed
 * into JSON.
 */
public class SerializableLibraryClasspathContainer {

  private String description;
  private String path;
  private SerializableClasspathEntry[] entries;

  public SerializableLibraryClasspathContainer(LibraryClasspathContainer container,
      IPath baseDirectory, IPath sourceBaseDirectory) {
    description = container.getDescription();
    path = container.getPath().toString();
    IClasspathEntry[] classpathEntries = container.getClasspathEntries();
    entries = new SerializableClasspathEntry[classpathEntries.length];
    for (int i = 0; i < classpathEntries.length; i++) {
      entries[i] =
          new SerializableClasspathEntry(classpathEntries[i], baseDirectory, sourceBaseDirectory);
    }
  }

  public LibraryClasspathContainer toLibraryClasspathContainer(IPath baseDirectory,
      IPath sourceBaseDirectory) {
    IClasspathEntry[] classpathEntries = new IClasspathEntry[entries.length];
    for (int i = 0; i < entries.length; i++) {
      classpathEntries[i] = entries[i].toClasspathEntry(baseDirectory, sourceBaseDirectory);
    }
    return new LibraryClasspathContainer(new Path(path), description, classpathEntries);
  }
}
