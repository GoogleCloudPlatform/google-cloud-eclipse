package com.google.cloud.tools.eclipse.ui.util.databinding;

import com.google.cloud.tools.eclipse.ui.util.Messages;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.util.regex.Pattern;

/**
 * Implements a simplified bucket name validation.
 * <p>
 * The following rules are verified:
 * <p>
 * Use lowercase letters, numbers, hyphens (-), and underscores (_). You can also use a dot (.) to form a valid
 * top-level domain (e.g., example.com). Format: You must start and end the name with a number or letter.
 * <p>
 * The actual rules that govern the bucket naming are more complex. See the complete list of bucket name requirements
 * for more information: https://cloud.google.com/storage/docs/naming
 */
public class BucketNameValidator implements IValidator {
  private static final Pattern CLOUD_STORAGE_BUCKET_NAME_PATTERN =
      Pattern.compile("^[a-z0-9][a-z0-9_.-]{1,61}[a-z0-9]$"); //$NON-NLS-1$

  @Override
  public IStatus validate(Object input) {
    if (!(input instanceof String)) {
      return ValidationStatus.error(Messages.getString("bucket.name.invalid")); //$NON-NLS-1$
    }
    String value = (String) input;
    if (value.isEmpty()) {
      return ValidationStatus.warning(Messages.getString("bucket.name.invalid")); //$NON-NLS-1$
    } else if (CLOUD_STORAGE_BUCKET_NAME_PATTERN.matcher((String) value).matches()) {
      return Status.OK_STATUS;
    } else {
      return ValidationStatus.error(Messages.getString("bucket.name.invalid")); //$NON-NLS-1$
    }
  }
}