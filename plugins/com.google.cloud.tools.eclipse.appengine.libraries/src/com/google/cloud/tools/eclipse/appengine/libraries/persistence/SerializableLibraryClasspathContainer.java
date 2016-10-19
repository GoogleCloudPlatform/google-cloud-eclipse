/*******************************************************************************
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
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.libraries.persistence;

import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainer;
import java.io.Serializable;
import java.util.ArrayList;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;

public class SerializableLibraryClasspathContainer implements Serializable {

  private static final long serialVersionUID = -3107083667953703399L;
  
  private String description;
  private String path;
  private ArrayList<SerializableClasspathEntry> entries;

  public SerializableLibraryClasspathContainer(LibraryClasspathContainer container) {
    description = container.getDescription();
    path = container.getPath().toOSString();
    entries = new ArrayList<>(container.getClasspathEntries().length);
    for (IClasspathEntry entry : container.getClasspathEntries()) {
      SerializableClasspathEntry serializableClasspathEntry = new SerializableClasspathEntry();
      serializableClasspathEntry.setAttributes(entry.getExtraAttributes());
      serializableClasspathEntry.setAccessRules(entry.getAccessRules());
      serializableClasspathEntry.setSourcePath(entry.getSourceAttachmentPath());
      serializableClasspathEntry.setPath(entry.getPath());
      entries.add(serializableClasspathEntry);
    }
  }

  public LibraryClasspathContainer toLibraryClasspathContainer() {
    IClasspathEntry[] classpathEntries = new IClasspathEntry[entries.size()];
    for (int i = 0; i < entries.size(); i++) {
      SerializableClasspathEntry serializableClasspathEntry = entries.get(i);
      IClasspathEntry entry = serializableClasspathEntry.toClasspathEntry();
      classpathEntries[i] = entry;
    }
    return new LibraryClasspathContainer(new Path(path), description, classpathEntries);
  }
}
