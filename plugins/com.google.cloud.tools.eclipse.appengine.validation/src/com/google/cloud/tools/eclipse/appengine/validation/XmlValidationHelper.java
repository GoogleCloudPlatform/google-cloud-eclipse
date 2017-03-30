package com.google.cloud.tools.eclipse.appengine.validation;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.w3c.dom.Document;

interface XmlValidationHelper {
  
  /**
   * Searches the {@link Document} for banned elements and returns them
   * as a list of {@link BannedElement}s.
   */
  ArrayList<BannedElement> checkForElements(IResource resource, Document document);
  
}
