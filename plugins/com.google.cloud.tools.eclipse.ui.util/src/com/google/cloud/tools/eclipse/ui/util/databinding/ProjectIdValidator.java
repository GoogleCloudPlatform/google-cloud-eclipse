package com.google.cloud.tools.eclipse.ui.util.databinding;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.google.cloud.tools.eclipse.ui.util.Messages;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;

public class ProjectIdValidator implements IValidator {
  private static final String APPENGINE_PROJECTID_PATTERN = "^[a-z][a-z0-9-]{5,29}$"; //$NON-NLS-1$

  @Override
  public IStatus validate(Object value) {
    String projectId = (String) value;
    if (projectId.matches(APPENGINE_PROJECTID_PATTERN)) {
      return Status.OK_STATUS;
    } else {
      return StatusUtil.error(this, Messages.getString("project.id.invalid"));  //$NON-NLS-1$
    }
  }
}