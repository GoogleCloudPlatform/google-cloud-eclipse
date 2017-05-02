/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.deploy.ui.internal;

import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import com.google.common.base.Preconditions;
import java.io.File;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

/**
 * Checks if a user-provided {@code app.yaml} path points to an existing {@code app.yaml}.
 * Validation will fail if the file does not exist, the path is not a file (e.g., a directory),
 * or the file name is not {@code app.yaml}. If the path to validate is not absolute, a preset
 * prefix path ({@code basePath}) will be appended prior to checking.
 *
 * @see #AppYamlPathValidator(IPath)
 */
public class AppYamlPathValidator implements IValidator {

  private final IPath basePath;

  public AppYamlPathValidator(IPath basePath) {
    Preconditions.checkArgument(basePath.isAbsolute());
    this.basePath = basePath;
  }

  @Override
  public IStatus validate(Object value) {
    Preconditions.checkArgument(value instanceof String);
    File appYaml = new File((String) value);
    if (!appYaml.isAbsolute()) {
      appYaml = new File(basePath + "/" + appYaml);
    }

    // appengine-plugins-core does not yet support other file names.
    if (!"app.yaml".equals(appYaml.getName())) {
      return ValidationStatus.error(Messages.getString("error.app.yaml.invalid.name", appYaml));
    } else if (!appYaml.exists()) {
      return ValidationStatus.error(Messages.getString("error.app.yaml.non.existing"));
    } else if (!appYaml.isFile()) {
      return ValidationStatus.error(Messages.getString("error.app.yaml.not.a.file", appYaml));
    } else {
      return ValidationStatus.ok();
    }
  }
}
