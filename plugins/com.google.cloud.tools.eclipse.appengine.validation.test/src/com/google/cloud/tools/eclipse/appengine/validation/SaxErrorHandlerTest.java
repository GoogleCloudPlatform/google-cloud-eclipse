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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXParseException;

public class SaxErrorHandlerTest {

  @Test
  public void testCreateSAXErrorMessage() throws CoreException {
    SAXParseException ex = new SAXParseException("message", "", "", 5, 13);
    IResource resource = Mockito.mock(IResource.class);
    IMarker marker = Mockito.mock(IMarker.class);
    Mockito.when(resource.createMarker(Mockito.anyString())).thenReturn(marker);
    
    SaxErrorHandler.createSaxErrorMessage(resource, ex);
    Mockito.verify(resource).createMarker("org.eclipse.core.resources.problemmarker");
  }

}
