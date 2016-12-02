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

package com.google.cloud.tools.eclipse.appengine.ui;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AppEngineJavaComponentMissingPageTest {

  private AppEngineJavaComponentMissingPage page;
  
  @Before
  public void setUp() {
    page = new AppEngineJavaComponentMissingPage("");
  }
  
  @Test
  public void testTitle() {
    Assert.assertEquals("App Engine Java Component is missing", page.getTitle());
  }
  
  @Test
  public void testDescription() {
    Assert.assertEquals("The Cloud SDK App Engine Java component is not installed",
        page.getErrorMessage());
  }

}
