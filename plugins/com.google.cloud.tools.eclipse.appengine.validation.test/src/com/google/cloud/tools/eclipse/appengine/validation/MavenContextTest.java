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

import java.util.Iterator;

import org.junit.Test;

public class MavenContextTest {
    
  private static final MavenContext CONTEXT = new MavenContext();
  
  @Test
  public void testGetNamespaceUri() {
    assertEquals("http://maven.apache.org/POM/4.0.0", CONTEXT.getNamespaceURI(""));
  }
  
  @Test
  public void testGetPrefix() {
    assertEquals("prefix", CONTEXT.getPrefix("http://maven.apache.org/POM/4.0.0"));
  }
  
  @Test
  public void testGetPrefixes() {
    Iterator<String> iterator = CONTEXT.getPrefixes("http://maven.apache.org/POM/4.0.0");
    assertEquals("prefix", iterator.next());
  }
}
