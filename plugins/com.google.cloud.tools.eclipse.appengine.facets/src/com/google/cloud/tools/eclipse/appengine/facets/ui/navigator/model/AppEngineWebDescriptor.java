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

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.StyledString;

/**
 * A model representation of the {@code appengine-web.xml}.
 */
public class AppEngineWebDescriptor extends AppEngineResourceElement {
  private final AppEngineDescriptor descriptor;

  public AppEngineWebDescriptor(IProject project, IFile file,
      AppEngineDescriptor descriptor) {
    super(project, file);
    this.descriptor = Preconditions.checkNotNull(descriptor);
  }

  @Override
  public StyledString getStyledLabel() {
    StyledString result = new StyledString("App Engine");
    result.append(" [standard", StyledString.QUALIFIER_STYLER);
    try {
      StyledString qualifier = new StyledString(": ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
      qualifier.append(
          Strings.isNullOrEmpty(descriptor.getRuntime()) ? "java7" : descriptor.getRuntime(),
          StyledString.QUALIFIER_STYLER);
      qualifier.append("]", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
      result.append(qualifier);
    } catch (AppEngineException ex) {
      // ignored
    }
    return result;
  }

  public AppEngineDescriptor getDescriptor() {
    return descriptor;
  }
}
