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

package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import org.junit.Assert;
import org.junit.Test;

public class MessagesTest {

  @Test
  public void testWizardTitle() {
    Assert.assertEquals(
        "Maven-based App Engine Standard Project", Messages.getString("WIZARD_TITLE"));
  }
  
  @Test
  public void testWizardDescription() {
    Assert.assertEquals(
        "Create a new Maven-based Eclipse project for App Engine standard environment development.",
        Messages.getString("WIZARD_DESCRIPTION"));
  }
  
  @Test
  public void testIllegalPackageName() {
    Assert.assertEquals("Illegal Java package name: com.example.foo",
        Messages.getString("ILLEGAL_PACKAGE_NAME", "com.example.foo"));
  }
}
