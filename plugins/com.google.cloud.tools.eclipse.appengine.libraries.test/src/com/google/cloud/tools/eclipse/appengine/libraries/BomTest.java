/*
 * Copyright 2018 Google LLC
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

package com.google.cloud.tools.eclipse.appengine.libraries;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class BomTest {

  private Element dependencyManagement;
  
  @Before
  public void setUp() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    DOMImplementation impl = builder.getDOMImplementation();

    Document doc = impl.createDocument("http://maven.apache.org/POM/4.0.0", "project", null);

    Node rootElement = doc.getDocumentElement();

    dependencyManagement =
        doc.createElementNS("http://maven.apache.org/POM/4.0.0", "dependencyManagement");
    Element dependencies = doc.createElementNS("http://maven.apache.org/POM/4.0.0", "dependencies");

    Element springDependency =
        configureBom(doc, "org.springframework.boot", "spring-boot-dependencies", "2.0.0.RELEASE");
    dependencies.appendChild(springDependency);
    Element cloudDependency = configureBom(doc, "com.google.cloud", "google-cloud-bom", "0.40.0-alpha");
    dependencies.appendChild(cloudDependency);
    dependencyManagement.appendChild(dependencies);
    rootElement.appendChild(dependencyManagement);
  }

  private Element configureBom(Document doc, String groupId, String artifactId, String version) {
    Element dependency = doc.createElementNS("http://maven.apache.org/POM/4.0.0", "dependency");
    Element groupIdElement = doc.createElementNS("http://maven.apache.org/POM/4.0.0", "groupId");
    Element artifactIdElement =
        doc.createElementNS("http://maven.apache.org/POM/4.0.0", "artifactId");
    Element versionElement = doc.createElementNS("http://maven.apache.org/POM/4.0.0", "version");

    groupIdElement.setTextContent(groupId);
    artifactIdElement.setTextContent(artifactId);
    versionElement.setTextContent(version);

    dependency.appendChild(groupIdElement);
    dependency.appendChild(artifactIdElement);
    dependency.appendChild(versionElement);
    return dependency;
  }

  @Test
  public void testCloudSpeech() {
    Assert.assertTrue(Bom.defines(dependencyManagement, "com.google.cloud", "google-cloud-speech"));
  }

  @Test
  public void testGroupdId() {
    Assert.assertFalse(Bom.defines(dependencyManagement, "com.google", "google-cloud-speech"));
  }
  
  @Test
  public void testCloudVision() {
    Assert.assertTrue(Bom.defines(dependencyManagement, "com.google.cloud", "google-cloud-vision"));
  }
  
  @Test
  public void testUnknown() {
    Assert.assertFalse(Bom.defines(dependencyManagement, "com.google.cloud", "unknown"));
  }

  @Test
  public void testGuava() {
    Assert.assertFalse(Bom.defines(dependencyManagement, "com.google.guava", "guava"));
  }
  
}
