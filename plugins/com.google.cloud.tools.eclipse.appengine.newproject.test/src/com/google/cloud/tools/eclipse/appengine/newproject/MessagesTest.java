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

package com.google.cloud.tools.eclipse.appengine.newproject;

import org.junit.Assert;
import org.junit.Test;

public class MessagesTest {

  @Test
  public void testNewAppEngineStandardProject() {
    Assert.assertEquals(
        "New App Engine Standard Project", 
        Messages.getString("new.app.engine.standard.project"));
  }
  
  @Test
  public void testPackageEndsWithPeriod() {
    Assert.assertEquals(
        "com.google. ends with a period.", 
        Messages.getString("package.ends.with.period", "com.google."));
  }
  
  @Test
  public void testWizardDescription() {
    Assert.assertEquals(
        "Create a new Eclipse project for App Engine standard environment development.",
        Messages.getString("create.app.engine.standard.project"));
  }

}
