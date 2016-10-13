package com.google.cloud.tools.eclipse.appengine.facets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Dependency;
import org.eclipse.core.runtime.IProgressMonitor;

import com.google.cloud.tools.eclipse.util.MavenUtils;

public class MavenAppEngineFacetUtil {
  /**
   * Returns a list of all the App Engine dependencies that should exist in the pom.xml
   * of a maven project that has the App Engine facet installed
   */
  public static List<Dependency> getAppEngineDependencies() {
    List<Dependency> dependencies = new ArrayList<Dependency>();

    Dependency appEngineApiDependency = new Dependency();
    appEngineApiDependency.setGroupId("com.google.appengine");
    appEngineApiDependency.setArtifactId("appengine-api-1.0-sdk");
    appEngineApiDependency.setVersion("${appengine.version}");
    appEngineApiDependency.setScope("");
    dependencies.add(appEngineApiDependency);

    Dependency servletApiDependency = new Dependency();
    servletApiDependency.setGroupId("javax.servlet");
    servletApiDependency.setArtifactId("servlet-api");
    servletApiDependency.setVersion("2.5");
    servletApiDependency.setScope("provided");
    dependencies.add(servletApiDependency);

    Dependency jstlDependency = new Dependency();
    jstlDependency.setGroupId("jstl");
    jstlDependency.setArtifactId("jstl");
    jstlDependency.setVersion("1.2");
    jstlDependency.setScope("provided");
    dependencies.add(jstlDependency);

    Dependency appEngineTestingDependency = new Dependency();
    appEngineTestingDependency.setGroupId("com.google.appengine");
    appEngineTestingDependency.setArtifactId("appengine-testing");
    appEngineTestingDependency.setVersion("${appengine.version}");
    appEngineTestingDependency.setScope("test");
    dependencies.add(appEngineTestingDependency);

    Dependency appEngineApiStubsDependency = new Dependency();
    appEngineApiStubsDependency.setGroupId("com.google.appengine");
    appEngineApiStubsDependency.setArtifactId("appengine-api-stubs");
    appEngineApiStubsDependency.setVersion("${appengine.version}");
    appEngineApiStubsDependency.setScope("test");
    dependencies.add(appEngineApiStubsDependency);

    return dependencies;
  }

  /**
   * Returns a map of all the App Engine properties that should exist in the pom.xml
   * of a maven project that has the App Engine facet installed
   *
   * @param monitor to be able to cancel operation
   * @return a map where the keys and values are the property fields and values respectively
   */
  public static Map<String, String> getAppEnginePomProperties(IProgressMonitor monitor) {
    String appengineArtifactVersion = MavenUtils.resolveLatestReleasedArtifactVersion(monitor,
        "com.google.appengine", "appengine-api-1.0-sdk", "jar", AppEngineStandardFacet.DEFAULT_APPENGINE_SDK_VERSION);
    String gcloudArtifactVersion = MavenUtils.resolveLatestReleasedArtifactVersion(monitor,
        "com.google.appengine", "gcloud-maven-plugin", "maven-plugin", AppEngineStandardFacet.DEFAULT_GCLOUD_PLUGIN_VERSION);

    Map<String, String> allProperties = new HashMap<String, String>();
    allProperties.put("app.id", "");
    allProperties.put("app.version", "1");
    allProperties.put("appengine.version", appengineArtifactVersion);
    allProperties.put("gcloud.plugin.version", gcloudArtifactVersion);
    return allProperties;
  }

}
