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