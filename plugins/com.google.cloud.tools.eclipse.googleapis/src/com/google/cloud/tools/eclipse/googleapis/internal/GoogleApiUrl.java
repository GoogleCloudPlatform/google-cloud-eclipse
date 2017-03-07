/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.googleapis.internal;

public enum GoogleApiUrl {

  APPENGINE_ADMIN_API("https://appengine.googleapis.com"),
  CLOUDRESOURCE_MANAGER_API("https://cloudresourcemanager.googleapis.com");

  private final String url;

  private GoogleApiUrl(String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }
}
