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

package com.google.cloud.tools.eclipse.appengine.libraries;

import com.google.cloud.tools.libraries.CloudLibraries;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.cloud.tools.libraries.json.CloudLibraryClient;
import com.google.cloud.tools.libraries.json.CloudLibraryClientMavenCoordinates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Element;

class Bom {
  
  private final static XPathFactory factory = XPathFactory.newInstance();

  private static Set<String> artifacts; 
  
  static {
    try {
      List<CloudLibrary> cloudLibraries = CloudLibraries.getCloudLibraries();
      Builder<String> setBuilder = ImmutableSet.builder();
      
      for (CloudLibrary library : cloudLibraries) {
        List<CloudLibraryClient> clients = library.getClients();
        if (clients != null) {
          for (CloudLibraryClient client : clients) {
            CloudLibraryClientMavenCoordinates coordinates = client.getMavenCoordinates();
            if (coordinates != null) {
              setBuilder.add(coordinates.getArtifactId()); 
            }
          }
        }
      }
      
      artifacts = setBuilder.build();
    } catch (IOException ex) {
      artifacts = new HashSet<>();
    }
  }
  
  static boolean defines(Element dependencyManagement, String groupId, String artifactId) {
    XPath xpath = factory.newXPath();
    xpath.setNamespaceContext(new Maven4NamespaceContext());

    try {
      String bomGroupId = (String) xpath.evaluate(
          "string(./m:dependencies/m:dependency[m:groupId='" + groupId + "']/m:groupId)",
          dependencyManagement,
          XPathConstants.STRING);
      String bomArtifactId = (String) xpath.evaluate(
          "string(./m:dependencies/m:dependency[m:groupId='" + groupId + "']/m:artifactId)",
          dependencyManagement,
          XPathConstants.STRING);

      // todo determine this dynamically by reading the BOMs to see if any include the relevant
      // artifact
      if ("com.google.cloud".equals(bomGroupId) && "google-cloud".equals(bomArtifactId)) {
        if ("com.google.cloud".equals(groupId)) {
          if (artifacts.contains(artifactId)) {
            return true;
          }
        }
      }
    } catch (XPathExpressionException ex) {
      return false;
    }
    return false;
  }

}
