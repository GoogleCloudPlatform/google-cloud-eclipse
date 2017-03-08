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

package com.google.cloud.tools.eclipse.ui.util.databinding;

import com.google.cloud.tools.eclipse.ui.util.Messages;
import com.google.common.base.Supplier;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

public class ProjectSelectorValidator implements IValidator {

  private Supplier<Boolean> projectsFound;

  /**
   * @param projectsFound returns true if there are projects in the project selector,
   * false otherwise
   */
  public ProjectSelectorValidator(Supplier<Boolean> projectsFound) {
    this.projectsFound = projectsFound;
  }

  @Override
  public IStatus validate(Object input) {
    if (!projectsFound.get()) {
      return ValidationStatus.error(Messages.getString("no.projects")); //$NON-NLS-1$
    }
    if (input == null || ((String) input).isEmpty()) {
      return ValidationStatus.error(Messages.getString("project.not.selected")); //$NON-NLS-1$
    }
    return ValidationStatus.ok();
  }
}
