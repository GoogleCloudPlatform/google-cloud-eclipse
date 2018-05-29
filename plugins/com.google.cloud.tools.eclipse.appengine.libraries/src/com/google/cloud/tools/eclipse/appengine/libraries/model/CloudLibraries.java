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

package com.google.cloud.tools.eclipse.appengine.libraries.model;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.RegistryFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class CloudLibraries {

  public static final String MASTER_CONTAINER_ID = "master-container";

  /**
   * Library files for App Engine Standard environment applications; specifically
   * Objectify, App Engine API, and Google Cloud Endpoints.
   */
  public static final String APP_ENGINE_STANDARD_GROUP = "appengine"; //$NON-NLS-1$

  /**
   * Library files for App Engine Flexible environment applications; specifically
   * Objectify.
   */
  public static final String APP_ENGINE_FLEXIBLE_GROUP = "flexible"; //$NON-NLS-1$  
  
  /**
   * Library files for the Google Cloud Client Library for Java. E.g.
   * Stackdriver Logging, Cloud Datastore, Cloud Storage, Cloud Translation, etc.
   */
  public static final String CLIENT_APIS_GROUP = "clientapis"; //$NON-NLS-1$

  private static final Logger logger = Logger.getLogger(CloudLibraries.class.getName());

  // Note: LibraryFile versions of Libraries in the map can be updated dynamically, e.g., to latest
  // available release versions.
  private static final ImmutableMap<String, Library> libraries = loadLibraryDefinitions();

  /**
   * Returns libraries in the named group.
   */
  public static List<Library> getLibraries(String group) {
    List<Library> result = new ArrayList<>();
    for (Library library : libraries.values()) {
      List<String> groups = library.getGroups();
      if (groups.contains(group)) {
        result.add(library);
      }
    }
    return result;
  }

  /**
   * Returns the library with the specified ID, or null if not found.
   */
  public static Library getLibrary(String id) {
    return libraries.get(id);
  }

  private static List<Library> loadClientApis() {
    Bundle bundle = FrameworkUtil.getBundle(CloudSdk.class);
    URL url = bundle.getResource("/com/google/cloud/tools/libraries/libraries.json");
    
    try (InputStream in = url.openStream();
        JsonReader reader = Json.createReader(in)) {
      JsonObject[] apis = reader.readArray().toArray(new JsonObject[0]); 
      List<Library> clientApis = new ArrayList<>(apis.length);
      for (JsonObject api : apis) {
        String name = api.getString("name");
        String id = api.getString("id");
        JsonArray transports = api.getJsonArray("transports");
        Library library = new Library(id);
        library.setGroup(CLIENT_APIS_GROUP);
        library.setName(name);
        // Currently there is exactly one transport per API.
        // This might or might not change in the future.
        library.setTransport(transports.getString(0));
        JsonArray clients = api.getJsonArray("clients");
        for (JsonObject client : clients.toArray(new JsonObject[0])) {
          try {
            JsonString language = client.getJsonString("language");
            if (language != null && "java".equals(language.getString())) {
              String toolTip = client.getString("infotip");
              library.setToolTip(toolTip);
              library.setLaunchStage(client.getString("launchStage"));
              JsonObject coordinates = client.getJsonObject("mavenCoordinates");
              String groupId = coordinates.getString("groupId");
              String artifactId = coordinates.getString("artifactId");
              String versionString = coordinates.getString("version");
  
              MavenCoordinates mavenCoordinates = new MavenCoordinates.Builder()
                  .setGroupId(groupId)
                  .setArtifactId(artifactId)
                  .setVersion(versionString)
                  .build();
              LibraryFile file = new LibraryFile(mavenCoordinates);
              List<LibraryFile> libraryFiles = new ArrayList<>();
              libraryFiles.add(file);
              library.setLibraryFiles(libraryFiles);
              break;
            }
          } catch (ClassCastException ex) {
            logger.log(Level.SEVERE, "Invalid libraries.json");
          }
        }
        clientApis.add(library);
      }
      
      return clientApis;
    } catch (IOException ex) {
      throw new RuntimeException("Could not read libraries.json", ex);
    }
  }
  
  private static ImmutableMap<String, Library> loadLibraryDefinitions() {
    IConfigurationElement[] elements = RegistryFactory.getRegistry().getConfigurationElementsFor(
        "com.google.cloud.tools.eclipse.appengine.libraries"); //$NON-NLS-1$
    ImmutableMap.Builder<String, Library> builder = ImmutableMap.builder();
    for (IConfigurationElement element : elements) {
      try {
        Library library = LibraryFactory.create(element);
        builder.put(library.getId(), library);
      } catch (LibraryFactoryException ex) {
        logger.log(Level.SEVERE, "Error loading library definition", ex); //$NON-NLS-1$
      }
    }

    for (Library library : loadClientApis()) {   
      builder.put(library.getId(), library);
    }
    
    ImmutableMap<String, Library> map = builder.build();

    resolveTransitiveDependencies(map);

    return map;
  }

  // Only goes one level deeper, which is all we need for now.
  // Does not recurse.
  private static void resolveTransitiveDependencies(ImmutableMap<String, Library> map) {
    for (Library library : map.values()) {
      List<String> directDependencies = library.getLibraryDependencies();
      List<String> transitiveDependencies = Lists.newArrayList(directDependencies);
      for (String id : directDependencies) {
        Library dependency = map.get(id);
        for (String dependencyId : dependency.getLibraryDependencies()) {
          if (!transitiveDependencies.contains(dependencyId)) {
            transitiveDependencies.add(dependencyId);
          }
        }
      }
      library.setLibraryDependencies(transitiveDependencies);
    }
  }
}
