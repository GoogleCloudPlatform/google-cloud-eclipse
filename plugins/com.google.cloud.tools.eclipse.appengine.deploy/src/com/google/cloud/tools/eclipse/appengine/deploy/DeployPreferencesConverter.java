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

package com.google.cloud.tools.eclipse.appengine.deploy;

import com.google.cloud.tools.appengine.api.deploy.DefaultDeployConfiguration;
import com.google.common.base.Strings;

public class DeployPreferencesConverter {

  private DeployPreferences preferences;

  public DeployPreferencesConverter(DeployPreferences preferences) {
    this.preferences = preferences;
  }

  public DefaultDeployConfiguration toDeployConfiguration() {
    DefaultDeployConfiguration configuration = new DefaultDeployConfiguration();

    configuration.setProject(preferences.getProjectId());

    String bucketName = preferences.getBucket();
    if (!Strings.isNullOrEmpty(bucketName)) {
      if (bucketName.startsWith("gs://")) {
        configuration.setBucket(bucketName);
      } else {
        configuration.setBucket("gs://" + bucketName);
      }
    }

    configuration.setPromote(preferences.isAutoPromote());
    if (preferences.isAutoPromote()) {
      configuration.setStopPreviousVersion(preferences.isStopPreviousVersion());
    }

    if (!Strings.isNullOrEmpty(preferences.getVersion())) {
      configuration.setVersion(preferences.getVersion());
    }

    return configuration;
  }
}
