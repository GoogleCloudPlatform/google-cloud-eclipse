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

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.archetype.catalog.Archetype;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.wizards.MappingDiscoveryJob;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.wst.common.componentcore.internal.builder.DependencyGraphImpl;
import org.eclipse.wst.common.componentcore.internal.builder.IDependencyGraph;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class CreateMavenBasedAppEngineStandardProject extends WorkspaceModifyOperation {

  private static final Logger logger =
      Logger.getLogger(CreateMavenBasedAppEngineStandardProject.class.getName());

  IProjectConfigurationManager projectConfigurationManager =
      MavenPlugin.getProjectConfigurationManager();

  private String packageName;
  private String artifactId;
  private String groupId;
  private String version;
  private IPath location;
  private Archetype archetype;
  private HashSet<String> appEngineLibraryIds = new HashSet<>();

  private List<IProject> archetypeProjects;
  private IFile mostImportant;

  /**
   * @return the file in the project that should be opened in an editor when the wizard finishes;
   *     may be null
   */
  IFile getMostImportant() {
    return mostImportant;
  }

  List<IProject> getArchetypeProjects() {
    return archetypeProjects;
  }

  @Override
  protected void execute(IProgressMonitor monitor)
      throws CoreException, InvocationTargetException, InterruptedException {
    SubMonitor progress = SubMonitor.convert(monitor, "Creating Maven AppEngine archetype", 110);

    String appengineArtifactVersion = MavenUtils.resolveLatestReleasedArtifactVersion(
        progress.newChild(20),
        "com.google.appengine", //$NON-NLS-1$
        "appengine-api-1.0-sdk", //$NON-NLS-1$
        "jar", //$NON-NLS-1$
        AppEngineStandardFacet.DEFAULT_APPENGINE_SDK_VERSION);
    String gcloudArtifactVersion = MavenUtils.resolveLatestReleasedArtifactVersion(
        progress.newChild(20),
        "com.google.appengine", //$NON-NLS-1$
        "gcloud-maven-plugin", //$NON-NLS-1$
        "maven-plugin", //$NON-NLS-1$
        AppEngineStandardFacet.DEFAULT_GCLOUD_PLUGIN_VERSION);

    Properties properties = new Properties();
    properties.put("appengine-version", appengineArtifactVersion); //$NON-NLS-1$
    properties.put("gcloud-version", gcloudArtifactVersion); //$NON-NLS-1$
    properties.put("useJstl", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    // The project ID is currently necessary due to tool bugs.
    properties.put("application-id", artifactId); //$NON-NLS-1$
    properties.put("useObjectify", //$NON-NLS-1$
        Boolean.toString(appEngineLibraryIds.contains("objectify"))); //$NON-NLS-1$
    properties.put("useEndpoints1", //$NON-NLS-1$
        Boolean.toString(appEngineLibraryIds.contains("appengine-endpoints"))); //$NON-NLS-1$
    properties.put("useEndpoints2", //$NON-NLS-1$
        "false"); //$NON-NLS-1$
    properties.put("useAppEngineApi", //$NON-NLS-1$
        Boolean.toString(appEngineLibraryIds.contains("appengine-api"))); //$NON-NLS-1$

    ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration();
    String packageName = this.packageName == null || this.packageName.isEmpty()
        ? null : this.packageName;

    // Workaround deadlock bug described in Eclipse bug (https://bugs.eclipse.org/511793).
    try {
      IDependencyGraph.INSTANCE.preUpdate();
      try {
        Job.getJobManager().join(DependencyGraphImpl.GRAPH_UPDATE_JOB_FAMILY,
            progress.newChild(10));
      } catch (OperationCanceledException | InterruptedException ex) {
        logger.log(Level.WARNING, "Exception waiting for WTP Graph Update job", ex);
      }

      archetypeProjects = projectConfigurationManager.createArchetypeProjects(location,
          archetype, groupId, artifactId, version, packageName, properties,
          importConfiguration, progress.newChild(40));
    } finally {
      IDependencyGraph.INSTANCE.postUpdate();
    }

    SubMonitor loopMonitor = progress.newChild(30).setWorkRemaining(3 * archetypeProjects.size());
    for (IProject project : archetypeProjects) {
      IFile pom = project.getFile("pom.xml");
      if (pom.exists()) {
        this.mostImportant = pom;
      }

      IFacetedProject facetedProject = ProjectFacetsManager.create(
          project, true, loopMonitor.newChild(1));
      AppEngineStandardFacet.installAppEngineFacet(facetedProject,
          true /* installDependentFacets */, loopMonitor.newChild(1));
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

  /** Set the package for any generated code; may be {@code null} */
  void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  /** Set the Maven artifact identifier for the generated project */
  void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  /** Set the Maven group identifier for the generated project */
  void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  /**
   * Set the location where the project is to be generated; may be {@code null} to indicate the
   * workspace
   */
  void setLocation(IPath location) {
    this.location = location;
  }

  /**
   * Set the version of the project to be created.
   */
  void setVersion(String version) {
    this.version = version;
  }

  void setArchetype(Archetype archetype) {
    this.archetype = archetype;
  }

  void setAppEngineLibraryIds(Collection<Library> libraries) {
    appEngineLibraryIds = new HashSet<>();
    for (Library library : libraries) {
      appEngineLibraryIds.add(library.getId());
    }
  }

}
