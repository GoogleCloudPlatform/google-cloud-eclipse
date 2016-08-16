package com.google.cloud.tools.eclipse.ui.util.databinding;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.google.cloud.tools.eclipse.ui.util.Messages;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;

public class ProjectIdValidator implements IValidator {
  private static final String APPENGINE_PROJECTID_PATTERN = "^[a-z][a-z0-9-]{5,29}$"; //$NON-NLS-1$
  private ValidationPredicate validationPredicate;

  public ProjectIdValidator(ValidationPredicate validationPredicate) {
    this.validationPredicate = validationPredicate;
  }

  @Override
  public IStatus validate(Object value) {
    if (!validationPredicate.shouldValidate()) {
      return Status.OK_STATUS;
    }
    String projectId = (String) value;
    if (projectId.matches(APPENGINE_PROJECTID_PATTERN)) {
      return Status.OK_STATUS;
    } else {
      return StatusUtil.error(this, Messages.getString("project.id.invalid"));  //$NON-NLS-1$
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