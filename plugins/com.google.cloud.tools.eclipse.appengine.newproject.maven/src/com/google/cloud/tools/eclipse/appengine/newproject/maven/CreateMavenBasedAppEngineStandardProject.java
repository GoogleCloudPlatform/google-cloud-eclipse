
package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.wizards.MappingDiscoveryJob;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineFacet;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CreateMavenBasedAppEngineStandardProject extends WorkspaceModifyOperation {
  
  private static final Logger logger =
      Logger.getLogger(CreateMavenBasedAppEngineStandardProject.class.getName());

  IProjectConfigurationManager projectConfigurationManager =
      MavenPlugin.getProjectConfigurationManager();

  private String appEngineProjectId;
  private String packageName;
  private String artifactId;
  private String groupId;
  private String version;
  private IPath location;
  private Archetype archetype;

  @Override
  protected void execute(IProgressMonitor monitor)
      throws CoreException, InvocationTargetException, InterruptedException {
    SubMonitor progress = SubMonitor.convert(monitor);
    monitor.beginTask("Creating Maven AppEngine archetype", 100);

    // todo: verify whether project ID is necessary during creation. The
    // archetype seems to require it so we use the artifact if unspecified.
    String appId = appEngineProjectId;
    if (appId == null || appId.trim().isEmpty()) {
      appId = artifactId;
    }
    String appengineArtifactVersion = resolveLatestReleasedArtifact(progress.newChild(20),
        "com.google.appengine", "appengine-api-1.0-sdk", "jar", "1.9.37");
    String gcloudArtifactVersion = resolveLatestReleasedArtifact(progress.newChild(20),
        "com.google.appengine", "gcloud-maven-plugin", "maven-plugin", "2.0.9.106.v20160420");

    Properties properties = new Properties();
    properties.put("appengine-version", appengineArtifactVersion);
    properties.put("gcloud-version", gcloudArtifactVersion);
    properties.put("application-id", appId);

    ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration();
    String packageName = this.packageName == null || this.packageName.isEmpty() 
        ? groupId : this.packageName;
    List<IProject> archetypeProjects = projectConfigurationManager.createArchetypeProjects(location,
        archetype, groupId, artifactId, version, packageName, properties,
        importConfiguration, progress.newChild(40));

    SubMonitor loopMonitor = progress.newChild(30).setWorkRemaining(3 * archetypeProjects.size());
    for (IProject project : archetypeProjects) {
      IFacetedProject facetedProject = ProjectFacetsManager.create(
          project, true, loopMonitor.newChild(1));
      AppEngineFacet.installAppEngineFacet(facetedProject, loopMonitor.newChild(1));  
      AppEngineFacet.installAppEngineRuntime(facetedProject, loopMonitor.newChild(1));  
    }
    
    /*
     * invoke the Maven lifecycle mapping discovery job
     * 
     * todo: is this step necessary? we know the archetype contents and we handle the
     * lifecycle-mappings and rules
     */
    Job job = new MappingDiscoveryJob(archetypeProjects);
    job.schedule();
  }

  private String resolveLatestReleasedArtifact(SubMonitor progress, String groupId,
      String artifactId, String type, String defaultVersion) {
    try {
      progress.beginTask(MessageFormat.format("Resolving latest version of {0}", artifactId), 10);
      String classifier = null;
      List<ArtifactRepository> artifactRepositories = null;
      Artifact artifact = MavenPlugin.getMaven().resolve(groupId, artifactId, "LATEST", type,
          classifier, artifactRepositories, progress.newChild(10));
      return artifact.getVersion();
    } catch (CoreException ex) {
      logger.log(Level.WARNING,
          MessageFormat.format("Unable to resolve artifact {0}:{1}", groupId, artifactId), ex);
      return defaultVersion;
    } finally {
      progress.done();
    }
  }

  /** Set the App Engine project identifier; may be {@code null} */
  public void setAppEngineProjectId(String appEngineProjectId) {
    this.appEngineProjectId = appEngineProjectId;
  }

  /** Set the package for any generated code; may be {@code null} */
  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  /** Set the Maven artifact identifier for the generated project */
  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  /** Set the Maven group identifier for the generated project */
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  /** Set the Maven version for the generated project */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Set the location where the project is to be generated; may be {@code null} to indicate the
   * workspace
   */
  public void setLocation(IPath location) {
    this.location = location;
  }

  public void setArchetype(Archetype archetype) {
    this.archetype = archetype;
  }
}
