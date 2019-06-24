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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.File;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;

public class DeployArtifactValidator extends FixedMultiValidator {

  private final IPath basePath;
  private final IObservableValue<String> deployArtifactPath;

  @VisibleForTesting
  DeployArtifactValidator(IPath basePath, IObservableValue<String> deployArtifactPath) {
    Preconditions.checkArgument(basePath.isAbsolute(), "basePath is not absolute.");
    Preconditions.checkArgument(String.class.equals(deployArtifactPath.getValueType()));
    this.basePath = basePath;
    this.deployArtifactPath = deployArtifactPath;
  }

  /**
   * Convenience constructor for {@link Text}.
   */
  public DeployArtifactValidator(IPath basePath, Text fileField) {
    this(basePath, WidgetProperties.text(SWT.Modify).observe(fileField));
  }

  @Override
  protected IStatus validate() {
    String pathValue = deployArtifactPath.getValue();
    if (pathValue.isEmpty()) {
      return ValidationStatus.error(Messages.getString("error.deploy.artifact.empty"));
    } else if (!pathValue.endsWith(".war") && !pathValue.endsWith(".jar")) {
      return ValidationStatus.error(Messages.getString("error.deploy.artifact.invalid.extension"));
    }

    File deployArtifact = new File(deployArtifactPath.getValue());
    if (!deployArtifact.isAbsolute()) {
      deployArtifact = new File(basePath + "/" + deployArtifact);
    }

    if (!deployArtifact.exists()) {
      return ValidationStatus.error(
          Messages.getString("error.deploy.artifact.non.existing", deployArtifact));
    } else if (!deployArtifact.isFile()) {
      return ValidationStatus.error(Messages.getString("error.not.a.file", deployArtifact));
    }
    return Status.OK_STATUS;
  }
}
