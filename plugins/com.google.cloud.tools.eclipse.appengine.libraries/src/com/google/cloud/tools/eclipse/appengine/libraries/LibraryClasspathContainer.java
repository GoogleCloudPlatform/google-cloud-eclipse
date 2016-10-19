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
package com.google.cloud.tools.eclipse.appengine.libraries;

import com.google.common.base.Preconditions;
import java.io.Serializable;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

public class LibraryClasspathContainer implements IClasspathContainer, Serializable {

  private static final long serialVersionUID = 2434038872899176357L;
  
  private final IPath containerPath;
  private final String description;
  private final IClasspathEntry[] classpathEntries;

  public LibraryClasspathContainer(IPath path, String description, IClasspathEntry[] classpathEntries) {
    Preconditions.checkNotNull(path, "path is null");
    Preconditions.checkNotNull(description, "description is null");
    Preconditions.checkArgument(!description.isEmpty(), "description is empty");
    Preconditions.checkNotNull(classpathEntries, "classpathEntries is null");
    containerPath = path;
    this.description = description;
    this.classpathEntries = classpathEntries;
  }

  @Override
  public IPath getPath() {
    return containerPath;
  }

  @Override
  public int getKind() {
    return IClasspathContainer.K_APPLICATION;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public IClasspathEntry[] getClasspathEntries() {
    return classpathEntries;
  }
}
