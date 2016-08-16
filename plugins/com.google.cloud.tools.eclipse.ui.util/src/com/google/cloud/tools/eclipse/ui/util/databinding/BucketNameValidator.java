package com.google.cloud.tools.eclipse.ui.util.databinding;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.google.cloud.tools.eclipse.ui.util.Messages;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;

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
  private static final String CLOUD_STORAGE_BUCKET_NAME_PATTERN = "^[a-z0-9][a-z0-9_.-]{1,61}[a-z0-9]$"; //$NON-NLS-1$
  private ValidationPredicate validationPredicate;

  public BucketNameValidator(ValidationPredicate validationPredicate) {
    this.validationPredicate = validationPredicate;
  }

  @Override
  public IStatus validate(Object value) {
    if (!validationPredicate.shouldValidate()) {
      return Status.OK_STATUS;
    }
    String projectId = (String) value;
    if (projectId.matches(CLOUD_STORAGE_BUCKET_NAME_PATTERN)) {
      return Status.OK_STATUS;
    } else {
      return StatusUtil.error(this, Messages.getString("bucket.name.invalid"));  //$NON-NLS-1$
    }
  }

  /**
   * If returns false, then the actual validation is skipped and {@link Status#OK_STATUS} is returned as validation
   * result.
   */
  public static interface ValidationPredicate {
    boolean shouldValidate();
  }
}