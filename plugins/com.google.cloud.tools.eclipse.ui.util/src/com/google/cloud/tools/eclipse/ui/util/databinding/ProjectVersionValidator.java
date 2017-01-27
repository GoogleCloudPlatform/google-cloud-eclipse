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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * Validate a project version.
 */
public class ProjectVersionValidator implements IValidator {
  private static final Pattern APPENGINE_PROJECT_VERSION_PATTERN =
      Pattern.compile("^([a-z0-9][a-z0-9-]{0,61}[a-z0-9]|[a-z0-9])$");

  private static final String RESERVED_PREFIX = "ah-";
  private static final List<String> RESERVED_VALUES = Arrays.asList("default", "latest");

  /**
   * @param value the prospective version string
   * @return OK status if valid, or an ERROR status with a description why invalid
   */
  @Override
  public IStatus validate(Object input) {
    // https://cloud.google.com/appengine/docs/java/config/appref
    // "The version specifier can contain lowercase letters, digits, and hyphens.
    // It cannot begin with the prefix ah- and the names default and latest are
    // reserved and cannot be used."
    if (!(input instanceof String)) {
      return ValidationStatus.error(Messages.getString("version.invalid")); //$NON-NLS-1$
    }
    String value = (String) input;
    if (value.isEmpty()) {
      return ValidationStatus.ok();
    } else if (APPENGINE_PROJECT_VERSION_PATTERN.matcher(value).matches()) {
      if (value.startsWith(RESERVED_PREFIX) || RESERVED_VALUES.contains(value)) {
        return ValidationStatus.error(Messages.getString("version.reserved")); //$NON-NLS-1$
      }
      return ValidationStatus.ok();
    } else {
      return ValidationStatus.error(Messages.getString("version.invalid")); //$NON-NLS-1$
    }
  }
}
