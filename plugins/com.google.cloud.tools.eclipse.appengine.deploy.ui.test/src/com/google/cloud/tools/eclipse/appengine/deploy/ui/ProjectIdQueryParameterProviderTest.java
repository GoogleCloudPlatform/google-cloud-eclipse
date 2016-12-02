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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.swt.widgets.Text;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ProjectIdQueryParameterProviderTest {

  private Text projectId;
  private ProjectIdQueryParameterProvider instance;
  
  @Before
  public void setUp() {
    projectId = Mockito.mock(Text.class);
    instance = new ProjectIdQueryParameterProvider(projectId);
  }
  
  @Test
  public void testEmpty() {
    Mockito.when(projectId.getText()).thenReturn("   ");
    Assert.assertTrue(instance.getParameters().isEmpty());
  }
  
  @Test
  public void testNotEmpty() {
    Mockito.when(projectId.getText()).thenReturn("myId");
    Assert.assertEquals(1, instance.getParameters().size());
    Assert.assertEquals("myId", instance.getParameters().get("project"));
  }

}
