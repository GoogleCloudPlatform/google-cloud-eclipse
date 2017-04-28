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

public class AppEngineDirectoryValidator implements IValidator {

  private final IPath basePath;

  public AppEngineDirectoryValidator(IPath basePath) {
    Preconditions.checkArgument(basePath.isAbsolute());
    this.basePath = basePath;
  }

  @Override
  public IStatus validate(Object value) {
    Preconditions.checkArgument(value instanceof String);
    File directory = new File((String) value);
    if (!directory.isAbsolute()) {
      directory = new File(basePath + "/" + directory);
    }

    if (new File(directory + "/app.yaml").exists()) {
      return ValidationStatus.ok();
    } else {
      return ValidationStatus.error(
          Messages.getString("error.invalid.app.engine.directory", directory));
    }
  }
}
