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

package com.google.cloud.tools.eclipse.usagetracker;

public class AnalyticsEvents {

  // Google Analytics event actions
  public static final String LOGIN_START = "user.login.start";
  public static final String LOGIN_SUCCESS = "user.login.success";
  public static final String LOGIN_CANCELED = "user.login.canceled";

  public static final String LIBRARY_SELECTED = "library.selected";

  public static final String DATAFLOW_RUN = "dataflow.run";
  public static final String DATAFLOW_NEW_PROJECT_WIZARD = "dataflow.new.project.wizard";
  public static final String DATAFLOW_NEW_PROJECT_WIZARD_COMPLETE =
      "dataflow.new.project.wizard.complete";

  public static final String APP_ENGINE_DEPLOY = "appengine.deploy";
  public static final String APP_ENGINE_DEPLOY_SUCCESS = "appengine.deploy.success";
  public static final String APP_ENGINE_LOCAL_SERVER = "appengine.local.server";
  public static final String APP_ENGINE_NEW_PROJECT_WIZARD = "appengine.new.project.wizard";
  public static final String APP_ENGINE_NEW_PROJECT_WIZARD_COMPLETE =
      "appengine.new.project.wizard.complete";

  public static final String CLOUD_SDK_INSTALL_SUCCESS = "cloud.sdk.install.success";
  public static final String CLOUD_SDK_COMPONENT_INSTALL_SUCCESS =
      "cloud.sdk.component.install.success";
  public static final String CLOUD_SDK_INSTALL_CANCELED = "cloud.sdk.install.canceled";
  public static final String CLOUD_SDK_INSTALL_FAILURE = "cloud.sdk.install.failure";
  public static final String CLOUD_SDK_UPDATE_SUCCESS = "cloud.sdk.update.success";
  public static final String CLOUD_SDK_UPDATE_CANCELED = "cloud.sdk.update.canceled";
  public static final String CLOUD_SDK_UPDATE_FAILURE = "cloud.sdk.update.failure";

  // Metadata keys
  public static final String APP_ENGINE_LOCAL_SERVER_MODE = "mode";
  public static final String APP_ENGINE_NEW_PROJECT_WIZARD_TYPE = "type";
  public static final String APP_ENGINE_NEW_PROJECT_WIZARD_BUILD_TOOL = "build.tool";
  public static final String APP_ENGINE_DEPLOY_STANDARD = "standard";
  public static final String APP_ENGINE_DEPLOY_FLEXIBLE = "flex";
  public static final String DATAFLOW_RUN_RUNNER = "runner";
  public static final String CLOUD_SDK_MANAGEMENT = "cloud.sdk.management";
  public static final String CLOUD_SDK_FAILURE_CAUSE = "cloud.sdk.failure.cause";

  public static final String PROJECT_TYPE = "project.type";
  public static final String LIBRARY_IDS = "library.ids";

  // Metadata values
  // This actually means "standard"; the value "native" is for a legacy reason.
  public static final String APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_STANDARD = "native";
  public static final String APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_FLEX = "flex";

  public static final String NATIVE_PROJECT = "native.project";
  public static final String MAVEN_PROJECT = "maven.project";

  public static final String AUTOMATIC_CLOUD_SDK = "automatic";
  public static final String MANUAL_CLOUD_SDK = "manual";
}
