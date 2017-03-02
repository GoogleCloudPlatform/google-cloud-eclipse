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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BannedElementTest {

  @Test(expected = NullPointerException.class)
  public void testBannedElementConstructor_nullElementName() {
    new BannedElement(null);
  }

  @Test(expected = NullPointerException.class)
  public void testBannedElementConstructor_nullLocation() {
    new BannedElement("test", "org.eclipse.core.resources.problemmarker", 1, null, 0);
  }
  
  @Test
  public void testEquals() {
    BannedElement element1 = new BannedElement("message");
    BannedElement element2 = new BannedElement("message");
    assertTrue(element1.equals(element2) && element2.equals(element1));
    
    BannedElement element3 =
        new BannedElement("message", "markerId_1", 1, new DocumentLocation(1, 1), 1);
    BannedElement element4 =
        new BannedElement("message", "markerId_2", 1, new DocumentLocation(1, 1), 1);
    assertFalse(element3.equals(element4));
    
    BannedElement element5 =
        new BannedElement("message", "markerId", 1, new DocumentLocation(1, 1), 1);
    BannedElement element6 =
        new BannedElement("message", "markerId", 1, new DocumentLocation(1, 2), 1);
    assertFalse(element5.equals(element6));
    
    BannedElement element7 =
        new BannedElement("message_1", "markerId", 1, new DocumentLocation(1, 1), 1);
    BannedElement element8 =
        new BannedElement("message_2", "markerId", 1, new DocumentLocation(1, 1), 1);
    assertFalse(element7.equals(element8));
  }
  
  @Test
  public void testHashCode() {
    BannedElement element1 = new BannedElement("message");
    BannedElement element2 = new BannedElement("message");
    assertTrue(element1.hashCode() == element2.hashCode());
  }

}

