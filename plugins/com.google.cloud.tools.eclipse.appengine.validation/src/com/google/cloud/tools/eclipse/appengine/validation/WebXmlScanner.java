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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
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
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Locator2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

/**
 * Scans web.xml for {@link BannedElement}s.
 */
class WebXmlScanner extends AbstractScanner {

  private static final Logger logger = Logger.getLogger(WebXmlScanner.class.getName());
  
  /** The {@code web.xml}'s JDT project; may be {@code null}. */
  private IJavaProject project;
  private boolean insideServletClassElement;
  private StringBuilder servletClassElementContents;
  private DocumentLocation servletClassElementLocation;
  
  WebXmlScanner(IFile file) {
    if (file != null) {
      project = JavaCore.create(file.getProject());
    }
  }
  
  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    // Checks for expected namespace URI. Assume something else is going on if
    // web.xml has an unexpected root namespace.
    if ("web-app".equals(localName) && ("http://xmlns.jcp.org/xml/ns/javaee".equals(uri)
        || "http://java.sun.com/xml/ns/javaee".equals(uri))) {
      String version = attributes.getValue("version");
      if (!version.equals("2.5")) {
    	// Adds <web-app> element to the queue if the Servlet version is not 2.5.
        Locator2 locator = getLocator();
        DocumentLocation start = new DocumentLocation(locator.getLineNumber(),
            locator.getColumnNumber());
        addToBlacklist(new JavaServletElement(Messages.getString("web.xml.version"), start, 0));
      }
    }
    if ("servlet-class".equals(localName)) {
      Locator2 locator = getLocator();
      insideServletClassElement = true;
      servletClassElementContents = new StringBuilder();
      servletClassElementLocation =
          new DocumentLocation(locator.getLineNumber(), locator.getColumnNumber());
    }
  }
  
  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (insideServletClassElement) {
      servletClassElementContents.append(ch, start, length);
    }
  }
  
  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if ("servlet-class".equals(localName)) {
      insideServletClassElement = false;
      String servletClassName = servletClassElementContents.toString();
      if (!classExists(project, servletClassName)) {
    	// Adds <servlet-class> element to the queue if the class doesn't exist in the project.
        addToBlacklist(new UndefinedServletElement(
            servletClassName, servletClassElementLocation, servletClassName.length()));
      }
    }
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
