/*
 * Copyright 2016 Google Inc.
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

import org.junit.Assert;
import org.junit.Test;

public class AppEngineJreWhitelistTest {

  @Test
  public void testNotWhitelisted() {
    Assert.assertFalse(AppEngineJreWhitelist.contains("java.net.CookieManager"));
  }
 
  @Test
  public void testWhitelisted() {
    Assert.assertTrue(AppEngineJreWhitelist.contains("java.lang.String"));
  }
  
  @Test
  public void testWhitelisted_nonJreClass() {
    Assert.assertTrue(AppEngineJreWhitelist.contains("com.google.Bar"));
  }
  
  @Test
  public void testWhitelisted_OmgClass() {
    Assert.assertFalse(AppEngineJreWhitelist.contains("org.omg.CosNaming.BindingIterator"));
  }
  
  @Test
  public void testWhitelisted_GssClass() {
    Assert.assertFalse(AppEngineJreWhitelist.contains("org.ietf.jgss.GSSContext"));
  }
  
  @Test
  public void testWhitelisted_JavaxClass() {
    Assert.assertTrue(AppEngineJreWhitelist.contains("javax.servlet.ServletRequest"));
  }
  
  @Test
  public void testWhitelisted_SwingClass() {
    Assert.assertFalse(AppEngineJreWhitelist.contains("javax.swing.JFrame"));
  }
  
}

