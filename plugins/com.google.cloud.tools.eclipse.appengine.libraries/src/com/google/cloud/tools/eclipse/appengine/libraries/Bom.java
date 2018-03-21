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

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Element;

/**
 * 
 */
class Bom {
  
  private final static XPathFactory factory = XPathFactory.newInstance();

  private static Set<String> artifacts = ImmutableSet.of("google-cloud-speech",
      "google-cloud-os-login",
      "google-cloud-dialogflow",
      "google-cloud-resourcemanager",
      "google-cloud-dlp",
      "google-cloud-video-intelligence",
      "google-cloud-monitoring",
      "google-cloud-errorreporting",
      "google-cloud-firestore",
      "google-cloud-trace",
      "google-cloud-dns",
      "google-cloud-vision",
      "google-cloud-language",
      "google-cloud-spanner",
      "google-cloud-pubsub",
      "google-cloud-bigquery",
      "google-cloud-datastore",
      "google-cloud-storage",
      "google-cloud-translate",
      "google-cloud-logging"); 
  
  // needs https://github.com/GoogleCloudPlatform/appengine-plugins-core/issues/569
  /* static {
    try {
      artifacts = com.google.cloud.tools.libraries.CloudLibraries.getCloudLibraries().stream()
          .map(CloudLibrary::getArtifactId)
          .collect(Collectors.toSet());
    } catch (IOException ex) {
      artifacts = new HashSet<>();
    }
  } */
  
  static boolean defines(Element dependencyManager, String groupId, String artifactId) {
    
    XPath xpath = factory.newXPath();
    xpath.setNamespaceContext(new Maven4NamespaceContext());

    try {
      String bomGroupId = (String) xpath.evaluate(
          "string(./m:dependencies/m:dependency/m:groupId)",
          dependencyManager,
          XPathConstants.STRING);
      String bomArtifactId = (String) xpath.evaluate(
          "string(./m:dependencies/m:dependency/m:artifactId)",
          dependencyManager,
          XPathConstants.STRING);

      // todo get these dynamically by reading the BOM
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
