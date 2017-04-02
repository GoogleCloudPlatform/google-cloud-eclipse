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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.RegistryFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class CloudLibraries {

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
  
  /**
   * Library files for all Java servlet applications; specifically
   * servlet.jar and jsp-api.jar.
   */
  public static final String SERVLET_GROUP = "servlet";
  
  private static final Logger logger = Logger.getLogger(CloudLibraries.class.getName());
  private static final ImmutableMap<String, Library> libraries = loadLibraryDefinitions();
  
  // todo consider caching maps of group to libraries
  /**
   * @return libraries in the named group
   */
  public static List<Library> getLibraries(String group) {
    List<Library> result = new ArrayList<>();
    for (Library library : libraries.values()) {
      if (library.getGroup().equals(group)) {
        result.add(library);
      }
    }
    return result;
  }
  
  /**
   * @return the library with the specified ID, or null if not found
   */
  public static Library getLibrary(String id) {
    return libraries.get(id);
  }
  
  private static ImmutableMap<String, Library> loadLibraryDefinitions() {
    IConfigurationElement[] elements = RegistryFactory.getRegistry().getConfigurationElementsFor(
        "com.google.cloud.tools.eclipse.appengine.libraries");
    LibraryFactory factory = new LibraryFactory();
    ImmutableMap.Builder<String, Library> builder = ImmutableMap.builder();
    for (IConfigurationElement element : elements) {
      try {
        Library library = factory.create(element);
        builder.put(library.getId(), library);
      } catch (LibraryFactoryException ex) {
        logger.log(Level.SEVERE, "Error loading library definition", ex);
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
