/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.libraries;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;

public class AppEngineLibraries {

  // TODO obtain libraries from extension registry
  // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/819
  public static List<Library> getAvailableLibraries() {
    Library appEngine = new Library("appengine-api");
    appEngine.setName("App Engine API");
    appEngine.setToolTip(Messages.getString("appengine.api.tooltip"));
    Library endpoints = new Library("appengine-endpoints");
    endpoints.setName("Google Cloud Endpoints");
    endpoints.setToolTip(Messages.getString("endpoints.tooltip"));
    endpoints.setLibraryDependencies(Collections.singletonList("appengine-api"));
    Library objectify = new Library("objectify");
    objectify.setName("Objectify");
    objectify.setToolTip(Messages.getString("objectify.tooltip"));
    objectify.setLibraryDependencies(Collections.singletonList("appengine-api"));
    return Arrays.asList(appEngine, endpoints, objectify);
  }
  
}
