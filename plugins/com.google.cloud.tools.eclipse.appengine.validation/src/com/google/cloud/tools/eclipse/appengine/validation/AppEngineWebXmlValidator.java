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
public class AppEngineWebXmlValidator implements XmlValidationHelper {

  @Override
  public ArrayList<ElementProblem> checkForProblems(IResource resource, Document document) {
    ArrayList<ElementProblem> problems = new ArrayList<>();

    List<ElementProblem> blacklistProblems = checkBlacklistedElements(document);
    problems.addAll(blacklistProblems);
    
    List<ElementProblem> runtimeProblems = checkRuntime(document);
    problems.addAll(runtimeProblems);
    
    return problems;
  }

  private static List<ElementProblem> checkBlacklistedElements(Document document) {   
    ArrayList<ElementProblem> problems = new ArrayList<>();
    ArrayList<String> blacklistedElements = AppEngineWebBlacklist.getBlacklistElements();
    for (String elementName : blacklistedElements) {
      NodeList nodeList =
          document.getElementsByTagNameNS("http://appengine.google.com/ns/1.0", elementName);
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        DocumentLocation userData = (DocumentLocation) node.getUserData("location");
        AppEngineBlacklistElement problem = new AppEngineBlacklistElement(
            elementName,
            userData,
            node.getTextContent().length());
        problems.add(problem);
      }
    }
    return problems;
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
      if ("java".equals(runtime)) {
        DocumentLocation userData = (DocumentLocation) runtimeElement.getUserData("location");
        ElementProblem problem = new ObsoleteRuntime("Java 6 runtime no longer supported", 
            userData, runtime.length());
        problems.add(problem);
      } else if ("java7".equals(runtime)) {
        DocumentLocation userData = (DocumentLocation) runtimeElement.getUserData("location");
        ElementProblem problem = new ObsoleteRuntime("Java 7 runtime no longer supported", 
            userData, runtime.length());
        problems.add(problem);
      }
    }
    
    if (nodeList.getLength() == 0) {
      ElementProblem problem = new MissingRuntime();
      problems.add(problem);
    }
    
    return problems;
  }
}
