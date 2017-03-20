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

package com.google.cloud.tools.eclipse.appengine.libraries.ui;

import org.junit.Assert;
import org.junit.Test;

public class MessagesTest {

  @Test
  public void testAppEngineTitle() {
    Assert.assertEquals("App Engine Standard Environment Libraries",  Messages.getString("appengine-title"));
  }
  
  @Test
  public void testClientApisTitle() {
    Assert.assertEquals("Google Client APIs for Java",  Messages.getString("clientapis-title"));
  }
  
  @Test
  public void testClientApisDescription() {
    Assert.assertEquals("Additional jars used by Google Client APIs for Java", 
        Messages.getString("clientapis-description"));
  }
  
  @Test
  public void testDescription() {
    Assert.assertEquals(
        "Additional jars commonly used in App Engine Standard Environment applications", 
        Messages.getString("appengine-description"));
  }
  

  @Test
  public void testUnknownMessage() {
    Assert.assertEquals("!foo.bar!",  Messages.getString("foo.bar"));
  }
  

}
