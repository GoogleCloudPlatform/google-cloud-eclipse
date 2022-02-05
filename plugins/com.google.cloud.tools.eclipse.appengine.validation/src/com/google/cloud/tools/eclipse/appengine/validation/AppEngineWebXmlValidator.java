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

package com.google.cloud.tools.eclipse.appengine.validation;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Validator for appengine-web.xml
 */
class AppEngineWebXmlValidator implements XmlValidationHelper {

  @Override
  public List<ElementProblem> checkForProblems(IResource resource, Document document) {
    ArrayList<ElementProblem> problems = new ArrayList<>();

    List<ElementProblem> deprecatedProblems = checkDeprecatedElements(document);
    problems.addAll(deprecatedProblems);
    
    List<ElementProblem> runtimeProblems = checkRuntime(document);
    problems.addAll(runtimeProblems);
    
    return problems;
  }

  private static List<ElementProblem> checkDeprecatedElements(Document document) {   
    ArrayList<ElementProblem> problems = new ArrayList<>();
    ArrayList<String> deprecatedElements = AppEngineWebProblems.getDeprecatedElements();
    for (String elementName : deprecatedElements) {
      NodeList nodeList =
          document.getElementsByTagNameNS("http://appengine.google.com/ns/1.0", elementName);
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        DocumentLocation location = (DocumentLocation) node.getUserData("location");
        
        // extend over the start-tag and end-tag
        int tagLength = node.getNodeName().length() + 2; // + 2 for < and >
        location = expandLocation(location, tagLength);
        int length = addTagLength(node, tagLength);
        
        AppEngineDeprecatedElement problem = new AppEngineDeprecatedElement(
            elementName,
            location,
            length);
        problems.add(problem);
      }
    }
    return problems;
  }

  // This is not a general purpose utility. 
  // It only works for simple elements with no attributes or child elements
  // and non insignificant white space in the start-tag. It does not work
  // for empty-element tags either.
  private static int addTagLength(Node node, int tagLength) {
    return node.getTextContent().length() + 2 * tagLength + 1; // +1 for the / in the end-tag
  }

  private static DocumentLocation expandLocation(DocumentLocation location, int tagLength) {
    int column = location.getColumnNumber() - tagLength;
    if (column < 0) {
      column = 0;
    }
    location = new DocumentLocation(location.getLineNumber(), column);
    return location;
  }

  /**
   * Check for obsolete runtimes.
   */
  private static List<ElementProblem> checkRuntime(Document document) {
    ArrayList<ElementProblem> problems = new ArrayList<>();
    NodeList nodeList =
        document.getElementsByTagNameNS("http://appengine.google.com/ns/1.0", "runtime");
    for (int i = 0; i < nodeList.getLength(); i++) {
      Element runtimeElement = (Element) nodeList.item(i);
      String runtime = runtimeElement.getTextContent();
      if ("java".equals(runtime) || "java7".equals(runtime)) {
        ElementProblem problem = makeRuntimeProblem(runtimeElement, runtime);
        problems.add(problem);
      }
      // else java8 and later are not a problem
    }
    
    if (nodeList.getLength() == 0) {
      DocumentLocation location =
          (DocumentLocation) document.getDocumentElement().getUserData("location");
      DocumentLocation expandedLocation = new DocumentLocation(location.getLineNumber(), 0);
      ElementProblem problem = new ObsoleteRuntime("Java 7 runtime no longer supported", 
          expandedLocation, "<appengine-web-app ".length());
      problems.add(problem);
    }
    
    return problems;
  }

  private static ElementProblem makeRuntimeProblem(Element runtimeElement, String runtime) {
    DocumentLocation location = (DocumentLocation) runtimeElement.getUserData("location");
    // extend over the start-tag and end-tag
    int tagLength = runtimeElement.getNodeName().length() + 2;
    DocumentLocation expandedLocation = expandLocation(location, tagLength);
    int length = addTagLength(runtimeElement, tagLength);
    
    String runtimeName = "java".equals(runtime) ? "Java 6" : "Java 7";
    ElementProblem problem = new ObsoleteRuntime(runtimeName + " runtime no longer supported", 
        expandedLocation, length);
    return problem;
  }
}
