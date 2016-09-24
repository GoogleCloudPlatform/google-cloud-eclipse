package com.google.cloud.tools.eclipse.ui.util.databinding;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IInputValidator;

import com.google.cloud.tools.eclipse.ui.util.Messages;
import com.google.cloud.tools.project.ProjectIdValidator;

public class ProjectIdInputValidator implements IValidator, IInputValidator {

  public enum ValidationPolicy {
    EMPTY_IS_VALID, EMPTY_IS_INVALID
  }

  public IStatus validate(Object input, ValidationPolicy policy) {
    if (!(input instanceof String)) {
      return ValidationStatus.error(Messages.getString("project.id.invalid")); //$NON-NLS-1$
    }
    String value = (String) input;
    return validateString(value, policy);
  }

  private IStatus validateString(String value, ValidationPolicy policy) {
    if (value.isEmpty()) {
      if (policy == ValidationPolicy.EMPTY_IS_INVALID) {
        return ValidationStatus.error(Messages.getString("project.id.empty")); //$NON-NLS-1$
      } else {
        return ValidationStatus.ok();
      }
    } else if (ProjectIdValidator.validate(value)) {
      return ValidationStatus.ok();
    } else {
      return ValidationStatus.error(Messages.getString("project.id.invalid")); //$NON-NLS-1$
    }
  }

  @Override
  public IStatus validate(Object input) {
    return validate(input, ValidationPolicy.EMPTY_IS_INVALID);
  }

  @Override
  public String isValid(String newText) {
    IStatus status = validateString(newText, ValidationPolicy.EMPTY_IS_INVALID);
    if (status.isOK()) {
      return null;
    } else {
      return status.getMessage();
    }
  }
}