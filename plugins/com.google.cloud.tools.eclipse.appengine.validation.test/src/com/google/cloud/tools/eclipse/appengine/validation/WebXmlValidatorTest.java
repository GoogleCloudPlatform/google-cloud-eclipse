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

import static org.mockito.Mockito.when;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;
import org.mockito.Mockito;

public class WebXmlValidatorTest {
 
  IFile file = Mockito.mock(IFile.class);
  
 @Test
 public void testValidate() throws CoreException, IOException, ParserConfigurationException {
   String markerId = "com.google.cloud.tools.eclipse.appengine.validation.servletMarker";
   when(file.createMarker(markerId)).thenReturn(Mockito.mock(IMarker.class));
   String xml = 
       "<web-app xmlns='http://xmlns.jcp.org/xml/ns/javaee' version='3.1'></web-app>";
   byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
   WebXmlValidator validator = new WebXmlValidator();
   validator.validate(file, bytes);
   Mockito.verify(file, Mockito.times(1)).createMarker(markerId);
 }
 
 @Test
 public void testValidate_noMarkers()
     throws CoreException, IOException, ParserConfigurationException {
   String xml = 
       "<web-app xmlns='http://java.sun.com/xml/ns/javaee' version='2.5'></web-app>";
   byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
   WebXmlValidator validator = new WebXmlValidator();
   validator.validate(file, bytes);
   String markerId = "com.google.cloud.tools.eclipse.appengine.validation.servletMarker";
   Mockito.verify(file, Mockito.times(0)).createMarker(markerId);
 }
 
}
