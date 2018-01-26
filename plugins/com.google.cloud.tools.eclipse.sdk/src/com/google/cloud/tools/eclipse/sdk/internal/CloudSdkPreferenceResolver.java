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

package com.google.cloud.tools.eclipse.sdk.internal;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdkResolver;
import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Google Cloud SDK locator that uses the user-configured location preference.
 */
public class CloudSdkPreferenceResolver implements CloudSdkResolver {
  private final IPreferenceStore preferences;

  public CloudSdkPreferenceResolver() {
    preferences = CloudSdkPreferences.getPreferenceStore();
  }

  @VisibleForTesting
  CloudSdkPreferenceResolver(IPreferenceStore preferences) {
    this.preferences = preferences;
  }

  @Override
  public Path getCloudSdkPath() {
    String value = preferences.getString(CloudSdkPreferences.CLOUD_SDK_PATH);
    if (value != null && !value.isEmpty()) {
      return Paths.get(value);
    }
    return null;
  }

  @Override
  public int getRank() {
    // since the user configures this path, this resolver should have highest priority
    return 0;
  }


}
