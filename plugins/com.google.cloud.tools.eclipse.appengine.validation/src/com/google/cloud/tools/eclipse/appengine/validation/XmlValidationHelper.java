package com.google.cloud.tools.eclipse.appengine.validation;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.w3c.dom.Document;

interface XmlValidationHelper {
  
  ArrayList<BannedElement> checkForElements(IResource resource, Document document);
  
}
