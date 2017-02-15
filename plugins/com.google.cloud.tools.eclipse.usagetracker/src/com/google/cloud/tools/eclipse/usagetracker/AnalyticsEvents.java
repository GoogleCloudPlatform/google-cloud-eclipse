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

  public static final String APP_ENGINE_DEPLOY = "appengine.deploy";
  public static final String APP_ENGINE_DEPLOY_SUCCESS = "appengine.deploy.success";
  public static final String APP_ENGINE_LOCAL_SERVER = "appengine.local.server";
  public static final String APP_ENGINE_NEW_PROJECT_WIZARD = "appengine.new.project.wizard";
  public static final String APP_ENGINE_NEW_PROJECT_WIZARD_COMPLETE =
      "appengine.new.project.wizard.complete";

  // Metadata keys
  public static final String APP_ENGINE_LOCAL_SERVER_MODE = "mode";
  public static final String APP_ENGINE_NEW_PROJECT_WIZARD_TYPE = "type";
  public static final String APP_ENGINE_DEPLOY_STANDARD = "standard";

  // Metadata values
  public static final String APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_NATIVE = "native";
  public static final String APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_MAVEN = "maven";
}
