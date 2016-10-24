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

package com.google.cloud.tools.eclipse.appengine.facets;

import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.junit.Assert;
import org.junit.Test;

public class MavenAppEngineFacetUtilTest {
  @Test
  public void testGetAppEngineDependencies() {
    List<Dependency> dependencies = MavenAppEngineFacetUtil.getAppEngineDependencies();

    Assert.assertNotNull(dependencies);
    Assert.assertEquals(5, dependencies.size());
    
    Dependency appengine = dependencies.get(0);
    Assert.assertEquals("com.google.appengine", appengine.getGroupId());
    Assert.assertEquals("appengine-api-1.0-sdk", appengine.getArtifactId());
    Assert.assertEquals("", appengine.getScope());
    Dependency servlet = dependencies.get(1);
    Assert.assertEquals("javax.servlet", servlet.getGroupId());
    Assert.assertEquals("provided", servlet.getScope());
    Dependency jstl = dependencies.get(2);
    Assert.assertEquals("jstl", jstl.getGroupId());
    Assert.assertEquals("provided", jstl.getScope());
    Dependency appengineTesting = dependencies.get(3);
    Assert.assertEquals("com.google.appengine", appengineTesting.getGroupId());
    Assert.assertEquals("appengine-testing", appengineTesting.getArtifactId());
    Dependency stubs = dependencies.get(4);
    Assert.assertEquals("com.google.appengine", stubs.getGroupId());
    Assert.assertEquals("appengine-api-stubs", stubs.getArtifactId());
  }

  @Test
  public void testGetAppEnginePomProperties() {
    Map<String, String> properties = MavenAppEngineFacetUtil.getAppEnginePomProperties(null /* monitor */);

    Assert.assertNotNull(properties);
    Assert.assertEquals(4, properties.size());
    Assert.assertTrue(properties.containsKey("app.id"));
    Assert.assertEquals("", properties.get("app.id"));
    Assert.assertTrue(properties.containsKey("app.version"));
    Assert.assertEquals("1", properties.get("app.version"));
    Assert.assertTrue(properties.containsKey("appengine.version"));
    Assert.assertTrue(properties.containsKey("gcloud.plugin.version"));
  }

}
