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
