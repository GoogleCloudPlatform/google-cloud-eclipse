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

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.RegistryFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.util.ArtifactRetriever;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class CloudLibraries {

  public static final String MASTER_CONTAINER_ID = "master-container";

  /**
   * Library files for App Engine Standard environment applications; specifically
   * Objectify, App Engine API, and Google Cloud Endpoints.
   */
  public static final String APP_ENGINE_GROUP = "appengine";   //$NON-NLS-1$

  /**
   * Library files for Google Client APIs for Java; specifically
   * google-api-client, oAuth, and google-http-client.
   */
  public static final String CLIENT_APIS_GROUP = "clientapis"; //$NON-NLS-1$

  private static final Logger logger = Logger.getLogger(CloudLibraries.class.getName());
  private static final ImmutableMap<String, Library> libraries = loadLibraryDefinitions();

  /**
   * Returns libraries in the named group.
   */
  public static List<Library> getLibraries(String group) {
    if (CLIENT_APIS_GROUP.equals(group)) {
      return clientApis.get();
    }
    
    List<Library> result = new ArrayList<>();
    for (Library library : libraries.values()) {
      if (library.getGroup().equals(group)) {
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

  private final static Supplier<List<Library>> clientApis = Suppliers.memoize(new Supplier<List<Library>>() {
    @Override
    public List<Library> get() {
      Bundle bundle = FrameworkUtil.getBundle(CloudSdk.class);
      URL url = bundle.getResource("/com/google/cloud/tools/libraries/libraries.json");
      
      try (InputStream in = url.openStream()) {
        JsonReader reader = Json.createReader(in); 
        JsonObject[] apis = reader.readArray().toArray(new JsonObject[0]); 
        List<Library> clientApis = new ArrayList<>(apis.length);
        for (JsonObject api : apis) {
          String name = api.getString("name");
          String id = api.getString("id");
          Library library = new Library(id);
          library.setGroup(CLIENT_APIS_GROUP);
          library.setName(name);
          JsonArray clients = api.getJsonArray("clients");
          for (JsonObject client : clients.toArray(new JsonObject[0])) {
            JsonString language = client.getJsonString("language");
            if (language != null && "java".equals(language.getString())) {
              String toolTip = client.getString("infotip");
              library.setToolTip(toolTip);
              JsonObject coordinates = client.getJsonObject("mavenCoordinates");
              String groupId = coordinates.getString("groupId");
              String artifactId = coordinates.getString("artifactId");
              ArtifactVersion version =
                  ArtifactRetriever.DEFAULT.getLatestArtifactVersion(groupId, artifactId);
              String versionString;
              if (version == null) {
                versionString = coordinates.getString("version");
                // todo need method to get latest nonrelease version instead for alphas and betas
              } else {
                versionString = version.toString();
              }
              
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
          }
          clientApis.add(library);
        }
        
        return clientApis;
      } catch (IOException ex) {
        throw new RuntimeException("Could not read libraries.json", ex);
      }
    }
  });
  
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
