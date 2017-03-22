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

package com.google.cloud.tools.eclipse.dataflow.core.project;

import org.eclipse.core.runtime.IPath;

/**
 * The result of validation a Dataflow Project, pre-creation.
 */
public enum DataflowProjectValidationStatus {
  /**
   * The project is valid and can be created with the current settings.
   */
  OK(true, null),
  /**
   * The location specified to put the project is not local.
   */
  LOCATION_NOT_LOCAL(false, "Location must be a local directory"),
  /**
   * The location specified to put the project does not exist.
   */
  NO_SUCH_LOCATION(false, "Location must be an existing directory"),
  /**
   * The location specified exists, but is not a directory.
   */
  LOCATION_NOT_DIRECTORY(false, "Location must be a directory"),
  /**
   * There is no provided maven group ID.
   */
  NO_GROUP_ID(false, "Enter a Group ID"),
  /**
   * The provided maven group ID is not valid.
   */
  ILLEGAL_GROUP_ID(false, "Invalid Group ID"),
  /**
   * There is no provided maven artifact ID.
   */
  NO_ARTIFACT_ID(false, "Enter an Artifact ID"),
  /**
   * The provided maven artifact ID is not valid.
   */
  ILLEGAL_ARTIFACT_ID(false, "Invalid Artifact ID"),
  /**
   * The Java package is invalid.
   */
  ILLEGAL_PACKAGE(false, "Invalid Java Package"),
  /**
   * The provided project name is not a valid segment. See {@link IPath#isValidSegment(String)}
   */
  PROJECT_NAME_NOT_SEGMENT(false, "Invalid Project Name"),
  /**
   * The target platform is not supported by the Dataflow SDK.
   */
  UNSUPPORTED_TARGET_PLATFORM(
      false,
      "Unsupported Target Platform."
      + " Please ensure that the JDK Compiler Compliance Level is set to 1.7 or higher.");

  private final boolean valid;
  private final String message;

  private DataflowProjectValidationStatus(boolean valid, String message) {
    this.valid = valid;
    this.message = message;
  }

  /**
   * Returns if the dataflow project being validated can be created with the current arguments.
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * Returns the error message for this {@link DataflowProjectValidationStatus}, or null if the
   * status is valid.
   */
  public String getMessage() {
    return message;
  }
}
