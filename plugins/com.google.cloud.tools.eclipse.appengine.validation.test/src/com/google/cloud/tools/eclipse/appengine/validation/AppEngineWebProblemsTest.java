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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

public class AppEngineWebProblemsTest {
  
  @Test
  public void testDeprecated() {
    Assert.assertTrue(AppEngineWebProblems.contains("application"));
  }
 
  @Test
  public void testContains() {
    assertFalse(AppEngineWebProblems.contains("foo"));
  }
  
  @Test
  public void testContains_nullArg() {
    assertFalse(AppEngineWebProblems.contains(null));
  }
  
  @Test(expected = NullPointerException.class)
  public void testGetDeprecatedElementMessage_nullArg() {
    AppEngineWebProblems.getDeprecatedElementMessage(null);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testGetDeprecatedElementMessage_unrelatedElement() {
    AppEngineWebProblems.getDeprecatedElementMessage("test");
  }
  
  @Test
  public void testGetDeprecatedElementMessage() {
    assertEquals("Project ID should be specified at deploy time",
      AppEngineWebProblems.getDeprecatedElementMessage("application"));
  }
  
  @Test
  public void testGetQuickAssistProcessor() {
    assertEquals(VersionQuickAssistProcessor.class.getName(),
        AppEngineWebProblems.getQuickAssistProcessor("version").getClass().getName());
  }
  
  @Test
  public void testGetDeprecatedElements() {
    ArrayList<String> elements = AppEngineWebProblems.getDeprecatedElements();
    assertEquals(2, elements.size());
    assertTrue("application".equals(elements.get(0)) ^ "application".equals(elements.get(1)));
    assertTrue("version".equals(elements.get(0)) ^ "version".equals(elements.get(1)));
  }
}

