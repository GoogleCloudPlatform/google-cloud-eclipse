package com.google.cloud.tools.eclipse.ui.util.databinding;

import java.util.regex.Pattern;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.google.cloud.tools.eclipse.ui.util.Messages;

public class ProjectIdValidator implements IValidator {
  // todo: this is different from .newproject.AppEngineProjectIdValidator
  // But this reflects the reality when creating a project which says:
  // Project ID must be between 6 and 30 characters.
  // Project ID can have lowercase letters, digits or hyphens and must start with a letter and
  // end with letter or digit
  // https://cloud.google.com/resource-manager/reference/rest/v1/projects
  // "It must be 6 to 30 lowercase letters, digits, or hyphens. It must start with a letter.
  // Trailing hyphens are prohibited."
  private static final Pattern APPENGINE_PROJECTID_PATTERN =
      Pattern.compile("^[a-z][a-z0-9-]{4,28}[a-z0-9]$"); //$NON-NLS-1$

  public enum ValidationPolicy {
    EMPTY_IS_VALID, EMPTY_IS_INVALID
  }

  public IStatus validate(Object input, ValidationPolicy policy) {
    if (!(input instanceof String)) {
      return ValidationStatus.error(Messages.getString("project.id.invalid")); //$NON-NLS-1$
    }
    String value = (String) input;
    if (value.isEmpty()) {
      if (policy == ValidationPolicy.EMPTY_IS_INVALID) {
        return ValidationStatus.error(Messages.getString("project.id.invalid")); //$NON-NLS-1$
      } else {
        return ValidationStatus.ok();
      }
    } else if (APPENGINE_PROJECTID_PATTERN.matcher(value).matches()) {
      return ValidationStatus.ok();
    } else {
      return ValidationStatus.error(Messages.getString("project.id.invalid")); //$NON-NLS-1$
    }
  }

  @Override
  public IStatus validate(Object input) {
    return validate(input, ValidationPolicy.EMPTY_IS_INVALID);
  }
}