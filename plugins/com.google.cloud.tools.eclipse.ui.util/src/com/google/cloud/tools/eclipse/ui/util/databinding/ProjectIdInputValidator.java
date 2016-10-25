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

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.google.cloud.tools.eclipse.ui.util.Messages;
import com.google.cloud.tools.project.ProjectIdValidator;

public class ProjectIdInputValidator implements IValidator {
  private boolean requireProjectId = true;

  public ProjectIdInputValidator(boolean requireProjectId) {
    this.requireProjectId = requireProjectId;
  }

  @Override
  public IStatus validate(Object input) {
    if (!(input instanceof String)) {
      return ValidationStatus.error(Messages.getString("project.id.invalid")); //$NON-NLS-1$
    }
    String value = (String) input;
    return validateString(value);
  }

  private IStatus validateString(String value) {
    if (value.isEmpty()) {
      return requireProjectId ?
          ValidationStatus.error(Messages.getString("project.id.empty")) : //$NON-NLS-1$
          ValidationStatus.ok();
    } else if (ProjectIdValidator.validate(value)) {
      return ValidationStatus.ok();
    } else {
      return ValidationStatus.error(Messages.getString("project.id.invalid")); //$NON-NLS-1$
    }
  }
}
