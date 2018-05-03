
package com.google.cloud.tools.eclipse.appengine.validation;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.xml.core.contentmodel.modelquery.IExternalSchemaLocationProvider;

/**
 * Associate schemas for App Engine standard environment configuration files.
 * 
 * @seealso the {@code org.eclipse.wst.xml.core.externalSchemaLocations} extension point
 */
public class AppEngineConfigFileSchemaLocationProvider implements IExternalSchemaLocationProvider {
  // Relevant XSDs are in this bundle
  private static final String SCHEMA_LOCATIONS_PREFIX =
      "platform:/plugin/com.google.cloud.tools.eclipse.appengine.validation/xsd/";

  @Override
  public Map<String, String> getExternalSchemaLocation(URI fileURI) {
    String basename = Path.fromPortableString(fileURI.getPath()).lastSegment();
    switch (basename) {
      case "appengine-web.xml":
        return Collections.singletonMap(SCHEMA_LOCATION,
            "http://appengine.google.com/ns/1.0 " + SCHEMA_LOCATIONS_PREFIX + "appengine-web.xsd");
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
        return null;
    }
  }


}
