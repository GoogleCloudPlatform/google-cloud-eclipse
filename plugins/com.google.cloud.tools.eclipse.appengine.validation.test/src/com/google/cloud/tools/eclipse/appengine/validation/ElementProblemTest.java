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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.resources.IMarker;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.junit.Test;

public class ElementProblemTest {

  @Test
  public void testElementProblemConstructor_nullElementName() {
    try {
      new ElementProblem(
          null,
          "org.eclipse.core.resources.problemmarker",
          IMarker.SEVERITY_WARNING,
          IMessage.NORMAL_SEVERITY,
          new DocumentLocation(4, 4),
          0,
          null);
      fail();
    } catch (NullPointerException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void testElementProblemConstructor_nullLocation() {
    try {
      new ElementProblem(
          "test",
          "org.eclipse.core.resources.problemmarker",
          IMarker.SEVERITY_WARNING,
          IMessage.NORMAL_SEVERITY,
          null,
          0,
          null);
      fail();
    } catch (NullPointerException ex) {
      assertNotNull(ex.getMessage());
    }
  }
  
  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void testEquals() {
    ElementProblem element1 = new ElementProblem(
        "message",
        "marker",
        IMarker.SEVERITY_WARNING,
        IMessage.NORMAL_SEVERITY,
        new DocumentLocation(4, 4),
        0,
        null);
    assertEquals(element1, element1);
    
    ElementProblem element2 = new ElementProblem(
        "message",
        "marker",
        IMarker.SEVERITY_WARNING,
        IMessage.NORMAL_SEVERITY,
        new DocumentLocation(4, 4),
        0,
        null);
    assertTrue(element1.equals(element2));
    assertTrue(element2.equals(element1));
    
    ElementProblem element3 =
        new ElementProblem(
          "message", 
          "markerId_1", 
          IMarker.SEVERITY_WARNING, 
          IMessage.NORMAL_SEVERITY,
          new DocumentLocation(1, 1),
          20,
          null);
    ElementProblem element4 =
        new ElementProblem(
          "message",
          "markerId_2",
          IMarker.SEVERITY_WARNING, 
          IMessage.NORMAL_SEVERITY, 
          new DocumentLocation(1, 1), 
          20, 
          null);
    assertFalse(element3.equals(element4));
    
    ElementProblem element5 =
        new ElementProblem(
          "message",
          "markerId", 
          IMarker.SEVERITY_WARNING, 
          IMessage.NORMAL_SEVERITY, 
          new DocumentLocation(1, 1),
          20, 
          null);
    ElementProblem element6 =
        new ElementProblem("message",
          "markerId", 
          IMarker.SEVERITY_WARNING, 
          IMessage.NORMAL_SEVERITY, 
          new DocumentLocation(1, 15),
          20, 
          null);
    assertFalse(element5.equals(element6));
    
    ElementProblem element7 =
        new ElementProblem("message_1",
          "markerId", 
          IMarker.SEVERITY_WARNING, 
          IMessage.NORMAL_SEVERITY, 
          new DocumentLocation(1, 1), 
          20,
          null);
    ElementProblem element8 =
        new ElementProblem("message_2", 
          "markerId", 
          IMarker.SEVERITY_WARNING, 
          IMessage.NORMAL_SEVERITY, 
          new DocumentLocation(1, 1), 
          20, 
          null);
    assertFalse(element7.equals(element8));
    
    assertFalse(element1.equals(null));
    assertFalse(element1.equals("test"));
  }
  
  @Test
  public void testHashCode() {
    ElementProblem element1 = new ElementProblem(
        "message",
        "marker",
        IMarker.SEVERITY_WARNING,
        IMessage.NORMAL_SEVERITY,
        new DocumentLocation(4, 4),
        0,
        null);
    ElementProblem element2 = new ElementProblem(
        "message",
        "marker",
        IMarker.SEVERITY_WARNING,
        IMessage.NORMAL_SEVERITY,
        new DocumentLocation(4, 4),
        0,
        null);;
    ElementProblem element3 = new ElementProblem(
        "message",
        "marker2",
        IMarker.SEVERITY_WARNING,
        IMessage.NORMAL_SEVERITY,
        new DocumentLocation(4, 4),
        0,
        null);;
    assertEquals(element1.hashCode(), element2.hashCode());
    assertNotEquals(element1.hashCode(), element3.hashCode());
  }

}

