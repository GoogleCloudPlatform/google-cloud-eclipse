package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import com.google.cloud.tools.appengine.api.deploy.DefaultDeployConfiguration;

public class StandardDeployPreferencesConverter {

  private StandardDeployPreferences preferences;

  public StandardDeployPreferencesConverter(StandardDeployPreferences preferences) {
    this.preferences = preferences;
  }

  public DefaultDeployConfiguration toDeployConfiguration() {
    DefaultDeployConfiguration configuration = new DefaultDeployConfiguration();

    configuration.setProject(preferences.getProjectId());

    if (preferences.isOverrideDefaultBucket()) {
      String bucketName = preferences.getBucket();
      if (bucketName.startsWith("gs://")) {
        configuration.setBucket(bucketName);
      } else {
        configuration.setBucket("gs://" + bucketName);
      }
    }

    configuration.setPromote(preferences.isAutoPromote());
    configuration.setStopPreviousVersion(preferences.isStopPreviousVersion());

    if (preferences.isOverrideDefaultVersioning()) {
      configuration.setVersion(preferences.getVersion());
    }

    return configuration;
  }
}
