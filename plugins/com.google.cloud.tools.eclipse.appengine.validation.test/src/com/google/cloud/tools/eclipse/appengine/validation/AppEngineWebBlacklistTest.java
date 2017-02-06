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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

public class AppEngineWebBlacklistTest {

  private static final String MESSAGE = "project ID tag not recommended";
  
  @Test
  public void testBlacklisted() {
    Assert.assertTrue(AppEngineWebBlacklist.contains("application"));
  }
 
  @Test
  public void testContains() {
    assertTrue(!AppEngineWebBlacklist.contains("foo"));
  }
  
  @Test
  public void testContains_nullArg() {
    assertTrue(!AppEngineWebBlacklist.contains(null));
  }
  
  @Test
  public void testGetBlacklistElementMessage_nullArg() {
    assertNotNull(AppEngineWebBlacklist.getBlacklistElementMessage(null));
  }
  
  @Test
  public void testGetBlacklistElementMessage_elementNotInBlacklist() {
    assertNotNull(AppEngineWebBlacklist.getBlacklistElementMessage("test"));
  }
  
  @Test
  public void testGetBlacklistElementMessage() { assertEquals(MESSAGE, AppEngineWebBlacklist.getBlacklistElementMessage("application"));
  }
}

