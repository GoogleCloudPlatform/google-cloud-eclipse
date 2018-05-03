/*
 * Copyright 2018 Google LLC
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

package com.google.cloud.tools.eclipse.appengine.validation;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.xml.core.contentmodel.modelquery.IExternalSchemaLocationProvider;

/**
 * Associate schemas for App Engine standard environment configuration files. Configured from the
 * {@code org.eclipse.wst.xml.core.externalSchemaLocations} extension point.
 */
public class AppEngineConfigFileSchemaLocationProvider implements IExternalSchemaLocationProvider {
  // Relevant XSDs are in this bundle
  private static final String SCHEMA_LOCATIONS_PREFIX =
      "platform:/plugin/com.google.cloud.tools.eclipse.appengine.validation/xsd/";

  @Override
  public Map<String, String> getExternalSchemaLocation(URI fileURI) {
    Preconditions.checkNotNull(fileURI);
    IPath path = Path.fromPortableString(fileURI.getPath());
    if (path.segmentCount() == 0) {
      return Collections.emptyMap();
    }
    String basename = path.lastSegment();
    switch (basename) {
      // appengine-web.xml has a known namespace
      case "appengine-web.xml":
        return ImmutableMap.<String, String>builder()
            .put(NO_NAMESPACE_SCHEMA_LOCATION, SCHEMA_LOCATIONS_PREFIX + "appengine-web.xsd")
            .put(SCHEMA_LOCATION, "http://appengine.google.com/ns/1.0 " + SCHEMA_LOCATIONS_PREFIX
                + "appengine-web.xsd")
            .build();

      case "datastore-indexes.xml":
      case "datastore-indexes-auto.xml":
        return Collections.singletonMap(NO_NAMESPACE_SCHEMA_LOCATION,
            SCHEMA_LOCATIONS_PREFIX + "datastore-indexes.xsd");

      case "cron.xml":
        return Collections.singletonMap(NO_NAMESPACE_SCHEMA_LOCATION,
            SCHEMA_LOCATIONS_PREFIX + "cron.xsd");

      case "dispatch.xml":
        return Collections.singletonMap(NO_NAMESPACE_SCHEMA_LOCATION,
            SCHEMA_LOCATIONS_PREFIX + "dispatch.xsd");

      case "queue.xml":
        return Collections.singletonMap(NO_NAMESPACE_SCHEMA_LOCATION,
            SCHEMA_LOCATIONS_PREFIX + "queue.xsd");

      case "dos.xml":
        return Collections.singletonMap(NO_NAMESPACE_SCHEMA_LOCATION,
            SCHEMA_LOCATIONS_PREFIX + "dos.xsd");

      default:
        return Collections.emptyMap();
    }
  }
}
