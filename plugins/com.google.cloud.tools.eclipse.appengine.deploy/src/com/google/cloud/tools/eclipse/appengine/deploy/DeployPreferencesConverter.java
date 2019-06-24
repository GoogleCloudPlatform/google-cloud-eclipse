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

import com.google.cloud.tools.appengine.configuration.DeployConfiguration;
import com.google.common.base.Strings;
import java.nio.file.Path;
import java.util.List;

class DeployPreferencesConverter {

  static DeployConfiguration toDeployConfiguration(DeployPreferences preferences,
      List<Path> deployables) {
    DeployConfiguration.Builder builder = DeployConfiguration.builder(deployables);

    builder.projectId(preferences.getProjectId());

    String bucketName = preferences.getBucket();
    if (!Strings.isNullOrEmpty(bucketName)) {
      if (bucketName.startsWith("gs://")) {
        builder.bucket(bucketName);
      } else {
        builder.bucket("gs://" + bucketName);
      }
    }

    builder.promote(preferences.isAutoPromote());
    if (preferences.isAutoPromote()) {
      builder.stopPreviousVersion(preferences.isStopPreviousVersion());
    }

    if (!Strings.isNullOrEmpty(preferences.getVersion())) {
      builder.version(preferences.getVersion());
    }

    return builder.build();
  }
}
