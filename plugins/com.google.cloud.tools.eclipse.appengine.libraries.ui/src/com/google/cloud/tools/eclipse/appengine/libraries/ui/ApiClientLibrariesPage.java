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

package com.google.cloud.tools.eclipse.appengine.libraries.ui;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineImages;
import com.google.common.collect.Maps;
import java.util.Map;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

public class ApiClientLibrariesPage extends CloudLibrariesPage {

  public ApiClientLibrariesPage() {
    super("clientapis");
    setTitle(Messages.getString("clientapis-title")); //$NON-NLS-1$
    setDescription(Messages.getString("apiclientlibrariespage-description")); //$NON-NLS-1$
    setImageDescriptor(AppEngineImages.appEngine(64));
  }

  @Override
  public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
    super.initialize(project, currentEntries);

    Map<String, String> groups = Maps.newLinkedHashMap();
    if (AppEngineStandardFacet.getProjectFacetVersion(project.getProject()) != null) {
      groups.put(CloudLibraries.APP_ENGINE_GROUP, Messages.getString("appengine-title"));
    }
    groups.put(CloudLibraries.CLIENT_APIS_GROUP, Messages.getString("clientapis-title"));
    setLibraryGroups(groups);
  }


}
