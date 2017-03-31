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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

/**
 * Validator for web.xml.
 */
public class WebXmlValidator implements XmlValidationHelper {
  
  private static final Logger logger = Logger.getLogger(WebXmlValidator.class.getName());
  
  @Override
  public ArrayList<BannedElement> checkForElements(IResource resource, Document document) {
    ArrayList<BannedElement> blacklist = new ArrayList<>();
    NodeList webAppList = document.getElementsByTagName("web-app");
    for (int i = 0; i < webAppList.getLength(); i++) {
      Element webApp = (Element) webAppList.item(i);
      String namespace = webApp.getNamespaceURI();
      String version = (String) webApp.getUserData("version");
      if ("http://xmlns.jcp.org/xml/ns/javaee".equals(namespace)
          || "http://java.sun.com/xml/ns/javaee".equals(namespace)) {
        if (!"2.5".equals(version)) {
          DocumentLocation location = (DocumentLocation) webApp.getUserData("location");
          BannedElement element = new JavaServletElement(location, 0);
          blacklist.add(element);
        }
      }
    }
    NodeList servletClassList = document.getElementsByTagName("servlet-class");
    for (int i = 0; i < servletClassList.getLength(); i++) {
      Node servletClassNode = servletClassList.item(i);
      String servletClassName = servletClassNode.getTextContent();
      IJavaProject project = getProject(resource);
      if (project != null && !classExists(project, servletClassName)) {
        DocumentLocation location = (DocumentLocation) servletClassNode.getUserData("location");
        BannedElement element =
            new UndefinedServletElement(servletClassName, location, servletClassName.length());
        blacklist.add(element);
      }
    }
    return blacklist;
  }
  
  private static IJavaProject getProject(IResource resource) {
    if (resource != null) {
      return JavaCore.create(resource.getProject());
    }
    return null;
  }

  @VisibleForTesting
  static boolean classExists(IJavaProject project, String typeName) {
    if (Strings.isNullOrEmpty(typeName)) {
      return false;
    }
    SearchPattern pattern = SearchPattern.createPattern(typeName, 
        IJavaSearchConstants.CLASS,
        IJavaSearchConstants.DECLARATIONS,
        SearchPattern.R_EXACT_MATCH | SearchPattern.R_ERASURE_MATCH);
    IJavaSearchScope scope = project == null ? SearchEngine.createWorkspaceScope()
        : SearchEngine.createJavaSearchScope(new IJavaElement[] {project});
    return performSearch(pattern, scope, null);
  }
  
  /**
   * Searches for a class that matches a pattern.
   */
  @VisibleForTesting
  static boolean performSearch(SearchPattern pattern, IJavaSearchScope scope,
      IProgressMonitor monitor) {
    try {
      SearchEngine searchEngine = new SearchEngine();
      TypeSearchRequestor requestor = new TypeSearchRequestor();
      searchEngine.search(pattern,
          new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
          scope, requestor, monitor);
      return requestor.foundMatch();
    } catch (CoreException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
      return false;
    }
  }

}
