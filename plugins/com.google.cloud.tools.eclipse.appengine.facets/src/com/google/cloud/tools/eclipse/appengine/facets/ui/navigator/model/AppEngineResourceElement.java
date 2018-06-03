/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model;

import com.google.common.base.Preconditions;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.StyledString;

/**
 * A representation for App Engine-specific configuration files. Amongst other things, it mplements
 * IAdaptable to expose the configuration file to enable Eclipse's <em>Open</em> functionality.
 */
public abstract class AppEngineResourceElement implements IAdaptable {
  private final IProject project;
  private final IFile file;

  public AppEngineResourceElement(IProject project, IFile file) {
    this.project = Preconditions.checkNotNull(project);
    this.file = Preconditions.checkNotNull(file);
    Preconditions.checkState(file.exists());
  }

  public IProject getProject() {
    return project;
  }

  public IFile getFile() {
    return file;
  }

  @Override
  public <T> T getAdapter(Class<T> adapter) {
    if (adapter.isInstance(file)) {
      return adapter.cast(file);
    }
    return null;
  }

  /** Return a styled description suitable for use in the Project Explorer. */
  public abstract StyledString getStyledLabel();

  /**
   * Offers an opportunity to return a
   * replacement for this instance, or {@code null} to remove.
   */
  public AppEngineResourceElement reload() {
    return file.exists() ? this : null;
  }
}
