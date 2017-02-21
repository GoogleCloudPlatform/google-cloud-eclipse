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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.RegistryFactory;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFactory;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFactoryException;

public class AppEngineLibraries {

  private static final Logger logger = Logger.getLogger(AppEngineLibraries.class.getName());
  
  // TODO obtain libraries by group name
  // TODO cache library list
  /**
   * @return the three core appengine libraries
   */
  public static List<Library> getAvailableLibraries() {
    IConfigurationElement[] elements = RegistryFactory.getRegistry().getConfigurationElementsFor(
        "com.google.cloud.tools.eclipse.appengine.libraries");
    LibraryFactory factory = new LibraryFactory();
    List<Library> result = new ArrayList<>();
    for (IConfigurationElement element : elements) {
      try {
        Library library = factory.create(element);
        if ("appengine".equals(library.getGroup())) {
          result.add(library);
        }
      } catch (LibraryFactoryException ex) {
        logger.log(Level.SEVERE, "Error loading library definition", ex);
      }
    }
    return result;
  }
  
}
