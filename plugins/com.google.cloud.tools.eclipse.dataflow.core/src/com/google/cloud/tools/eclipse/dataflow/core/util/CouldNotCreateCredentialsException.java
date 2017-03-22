/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.util;

import com.google.cloud.tools.eclipse.dataflow.core.DataflowCorePlugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.io.IOException;

/**
 * Signals that credentials could not be created.
 */
public class CouldNotCreateCredentialsException extends IOException {
  public CouldNotCreateCredentialsException(String message, Exception cause) {
    super(message, cause);
  }

  public IStatus getStatus() {
    return new Status(Status.ERROR, DataflowCorePlugin.PLUGIN_ID, getMessage());
  }
}
