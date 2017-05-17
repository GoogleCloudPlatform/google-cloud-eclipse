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
import com.google.cloud.tools.project.AppYaml;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

/**
 * Checks an {@code app.yaml} path and the specified runtime in it.
 *
 * 1. Checks if a user-provided {@code app.yaml} path points to an existing {@code app.yaml}.
 * Validation will fail if the file does not exist, the path is not a file (e.g., a directory),
 * or the file name is not {@code app.yaml}. If the path to validate is not absolute, a preset
 * prefix path ({@code basePath}) will be appended prior to checking.
 *
 * 2. Checks if the runtime specified in {@code app.yaml} is supported.
 */
public class AppYamlValidator extends FixedMultiValidator {

  private final IPath basePath;
  private final IObservableValue appYamlPath;

  public AppYamlValidator(IPath basePath, IObservableValue appYamlPath) {
    Preconditions.checkArgument(basePath.isAbsolute(), "basePath is not absolute.");
    Preconditions.checkArgument(String.class.equals(appYamlPath.getValueType()));
    this.basePath = basePath;
    this.appYamlPath = appYamlPath;
  }

  @Override
  protected IStatus validate() {
    File appYaml = new File((String) appYamlPath.getValue());
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
      return validateRuntime(appYaml);
    }
  }

  @VisibleForTesting
  static IStatus validateRuntime(File appYamlFile) {
    try {
      AppYaml appYaml = new AppYaml(appYamlFile.toPath());
      String runtime = appYaml.getRuntime();

      if ("custom".equals(runtime)) {
        return ValidationStatus.error(Messages.getString("error.app.yaml.custom.runtime"));
      } else if (!"java".equals(runtime)) {
        return ValidationStatus.error(
            Messages.getString("error.app.yaml.not.java.runtime", runtime));
      } else {
        return ValidationStatus.ok();
      }
    } catch (ScannerException | ParserException ex) {
      return ValidationStatus.error(Messages.getString("error.app.yaml.malformed"));
    } catch (IOException ex) {
      return ValidationStatus.error(
          Messages.getString("error.app.yaml.cannot.read", ex.getLocalizedMessage()));
    }
  }
}
