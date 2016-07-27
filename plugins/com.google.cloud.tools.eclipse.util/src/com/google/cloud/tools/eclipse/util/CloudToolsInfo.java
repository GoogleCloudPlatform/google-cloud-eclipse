package com.google.cloud.tools.eclipse.util;

import org.osgi.framework.FrameworkUtil;

/**
 * Provides generic information about our collective tools, such as a name to be used for usage
 * reporting and the current version, etc.
 */
public class CloudToolsInfo {
  
  // Don't change the value; this name is used as an originating "application" of usage metrics.
  public static String TOOLS_NAME_FOR_METRICS = "gcloud-eclipse-tools";

  public static String getToolsVersion() {
    return FrameworkUtil.getBundle(new CloudToolsInfo().getClass()).getVersion().toString();
  }
}
