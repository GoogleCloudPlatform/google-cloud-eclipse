package com.google.cloud.tools.eclipse.ui.util.databinding;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.google.cloud.tools.eclipse.ui.util.Messages;
import com.google.cloud.tools.project.ProjectIdValidator;

public class ProjectIdInputValidator implements IValidator {

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
      return ValidationStatus.error(Messages.getString("project.id.empty")); //$NON-NLS-1$
    } else if (ProjectIdValidator.validate(value)) {
      return ValidationStatus.ok();
    } else {
      return ValidationStatus.error(Messages.getString("project.id.invalid")); //$NON-NLS-1$
    }
  }
}