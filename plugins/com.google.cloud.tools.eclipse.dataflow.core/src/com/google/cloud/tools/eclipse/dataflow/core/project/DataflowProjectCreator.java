/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.project;

import com.google.cloud.tools.eclipse.dataflow.core.DataflowCorePlugin;
import com.google.cloud.tools.eclipse.dataflow.core.natures.DataflowJavaProjectNature;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.WritableDataflowPreferences;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSortedSet;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An {@code IRunnableWithProgress} that creates a new Cloud Dataflow Java Project.
 */
public class DataflowProjectCreator implements IRunnableWithProgress {
  private static final Pattern MAVEN_ID_REGEX = Pattern.compile("[A-Za-z0-9_\\-.]+");
  private static final Pattern JAVA_PACKAGE_REGEX =
      Pattern.compile("([a-zA-Z_$][a-zA-Z0-9_$]*\\.)*[a-zA-Z_$][a-zA-Z0-9_$]*");

  private static final String DEFAULT_JAVA_VERSION = JavaCore.VERSION_1_7;
  private static final List<String> JAVA_VERSION_BLACKLIST =
      Collections.unmodifiableList(Arrays.asList(JavaCore.VERSION_1_1, JavaCore.VERSION_1_2,
          JavaCore.VERSION_1_3, JavaCore.VERSION_1_4, JavaCore.VERSION_1_5, JavaCore.VERSION_1_6,
          JavaCore.VERSION_CLDC_1_1));

  private final DataflowArtifactRetriever artifactRetriever;
  private final IProjectConfigurationManager projectConfigurationManager;

  private Template template;
  // TODO: Configure in constructor
  private MajorVersion majorVersion = MajorVersion.ONE;
  private String projectNameTemplate;
  private boolean customLocation;
  private URI projectLocation;
  private String mavenGroupId;
  private String mavenArtifactId;
  private String packageString;
  private String archetypeVersion;

  private String defaultProject;
  private String defaultStagingLocation;


  /**
   * Enumeration of the Archetype templates available for project creation.
   */
  public enum Template {
    STARTER_POM_WITH_PIPELINE(
        "Starter project with a simple pipeline",
        "google-cloud-dataflow-java-archetypes-starter",
        ImmutableSortedSet.of(MajorVersion.ONE, MajorVersion.QUALIFIED_TWO, MajorVersion.TWO)),
    EXAMPLES(
        "Example pipelines",
        "google-cloud-dataflow-java-archetypes-examples",
        ImmutableSortedSet.of(MajorVersion.ONE, MajorVersion.QUALIFIED_TWO, MajorVersion.TWO));

    private final String label;
    private final String archetype;
    private final ImmutableSortedSet<MajorVersion> sdkVersions;

    Template(String label, String archetype, NavigableSet<MajorVersion> sdkVersions) {
      this.label = label;
      this.archetype = archetype;
      this.sdkVersions = ImmutableSortedSet.copyOf(sdkVersions);
    }

    public String getLabel() {
      return label;
    }

    public String getArchetype() {
      return archetype;
    }

    public NavigableSet<MajorVersion> getSdkVersions() {
      return sdkVersions;
    }
  }

  DataflowProjectCreator(
      DataflowArtifactRetriever artifactRetriever,
      IProjectConfigurationManager projectConfigurationManager) {
    this.artifactRetriever = artifactRetriever;
    this.projectConfigurationManager = projectConfigurationManager;

    template = Template.STARTER_POM_WITH_PIPELINE;
  }

  public static DataflowProjectCreator create() {
    return new DataflowProjectCreator(
        DataflowArtifactRetriever.defaultInstance(), MavenPlugin.getProjectConfigurationManager());
  }

  public Collection<DataflowProjectValidationStatus> validate() {
    return failedValidations();
  }

  public boolean isValid() {
    return failedValidations().isEmpty();
  }

  public void setProjectNameTemplate(String projectName) {
    this.projectNameTemplate = projectName;
  }

  public void setPackage(String packageString) {
    this.packageString = packageString;
  }

  public void setCustomLocation(boolean customLocation) {
    this.customLocation = customLocation;
  }

  public void setProjectLocation(URI projectLocation) {
    this.projectLocation = projectLocation;
  }

  public void setMavenGroupId(String mavenGroupId) {
    this.mavenGroupId = mavenGroupId;
  }

  public void setMavenArtifactId(String mavenArtifactId) {
    this.mavenArtifactId = mavenArtifactId;
  }

  public void setTemplate(Template template) {
    this.template = template;
  }

  public void setArchetypeVersion(String archetypeVersion) {
    this.archetypeVersion = archetypeVersion;
  }

  public void setDefaultProject(String defaultProject) {
    this.defaultProject = defaultProject;
  }

  public void setDefaultStagingLocation(String defaultStagingLocation) {
    this.defaultStagingLocation = defaultStagingLocation;
  }

  @Override
  public void run(IProgressMonitor monitor)
      throws InvocationTargetException, InterruptedException, OperationCanceledException {
    if (!isValid()) {
      throw new ProjectCreationException(
          "Cannot create a project with invalid or incomplete inputs",
          "Validation Failures: " + validate());
    }
    SubMonitor progress = SubMonitor.convert(monitor, 5);
    checkCancelled(progress);

    ProjectImportConfiguration projectImportConfiguration = new ProjectImportConfiguration();
    if (projectNameTemplate != null) {
      projectImportConfiguration.setProjectNameTemplate(projectNameTemplate);
    }

    checkCancelled(progress);

    Archetype archetype = new Archetype();
    archetype.setGroupId(DataflowArtifactRetriever.DATAFLOW_GROUP_ID);
    archetype.setArtifactId(template.getArchetype());

    Properties archetypeProperties = new Properties();
    archetypeProperties.setProperty("targetPlatform", getTargetPlatform());

    IPath location = null;
    if (customLocation) {
      location = org.eclipse.core.filesystem.URIUtil.toPath(projectLocation);
    }

    Set<ArtifactVersion> archetypeVersions;
    if (Strings.isNullOrEmpty(archetypeVersion)) {
      // TODO: Configure the creator with a targeted Major Version
      archetypeVersions = defaultArchetypeVersions(template, majorVersion);
    } else {
      archetypeVersions =
          Collections.<ArtifactVersion>singleton(new DefaultArtifactVersion(archetypeVersion));
    }

    List<IProject> projects = Collections.emptyList();
    List<CoreException> failures = new ArrayList<>();
    for (ArtifactVersion attemptedVersion : archetypeVersions) {
      checkCancelled(progress);
      // TODO: See if this can be done without using the toString method
      archetype.setVersion(attemptedVersion.toString());
      try {
        projects = projectConfigurationManager.createArchetypeProjects(location, archetype,
            // TODO: Get the version string from the user as well.
            mavenGroupId, mavenArtifactId, "0.0.1-SNAPSHOT", packageString, archetypeProperties,
            projectImportConfiguration, progress.newChild(4));
        break;
      } catch (CoreException e) {
        failures.add(e);
      }
    }
    if (projects.isEmpty()) {
      for (CoreException failure : failures) {
        DataflowCorePlugin.logError(failure, "CoreException while creating new Dataflow Project");
      }
    }

    SubMonitor natureMonitor = SubMonitor.convert(progress.newChild(1), projects.size());
    for (IProject project : projects) {
      try {
        DataflowJavaProjectNature.addDataflowJavaNatureToProject(
            project, natureMonitor.newChild(1));
        setPreferences(project);
      } catch (CoreException e) {
        DataflowCorePlugin.logError(e,
            "CoreException while adding Dataflow Nature to created project %s", project.getName());
      }
    }
    monitor.done();
  }

  private Set<ArtifactVersion> defaultArchetypeVersions(Template template, MajorVersion mv) {
    ArtifactVersion latestArchetype = artifactRetriever.getLatestArchetypeVersion(template, mv);
    return Collections.singleton(
        latestArchetype == null ? mv.getInitialVersion() : latestArchetype);
  }

  private void setPreferences(IProject project) {
    WritableDataflowPreferences prefs = WritableDataflowPreferences.forProject(project);
    prefs.setDefaultProject(this.defaultProject);
    prefs.setDefaultStagingLocation(this.defaultStagingLocation);
    prefs.save();
  }

  /**
   * Gets the target platform of the environment's JDT plugin. If not found, use the default target
   * platform. If not supported, throw a {@link ProjectCreationException}.
   */
  private String getTargetPlatform() throws ProjectCreationException {
    // Safe cast by API Contract
    String targetPlatform = JavaCore.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);
    if (targetPlatform == null || JAVA_VERSION_BLACKLIST.contains(targetPlatform)) {
      DataflowCorePlugin.logWarning(
          "Couldn't find supported target platform. Got %s. Using default version %s",
          targetPlatform, DEFAULT_JAVA_VERSION);
      targetPlatform = DEFAULT_JAVA_VERSION;
    } else {
      targetPlatform = targetPlatform.toString();
    }
    return targetPlatform;
  }

  private DataflowProjectValidationStatus validateTargetPlatform() {
    try {
      getTargetPlatform();
      return DataflowProjectValidationStatus.OK;
    } catch (ProjectCreationException e) {
      return DataflowProjectValidationStatus.UNSUPPORTED_TARGET_PLATFORM;
    }
  }

  private void checkCancelled(IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }
  }

  /**
   * Returns all of the validation failures in this {@code DataflowProject}.
   */
  public Collection<DataflowProjectValidationStatus> failedValidations() {
    Collection<DataflowProjectValidationStatus> statuses =
        EnumSet.noneOf(DataflowProjectValidationStatus.class);
    statuses.add(validateProjectName());
    statuses.add(validateProjectLocation());
    statuses.add(validateMavenGroupId());
    statuses.add(validateMavenArtifactId());
    statuses.add(validatePackage());
    statuses.add(validateTargetPlatform());

    // Must be last: Remove OK so the set contains only failed validations.
    statuses.remove(DataflowProjectValidationStatus.OK);
    return statuses;
  }

  private DataflowProjectValidationStatus validateMavenArtifactId() {
    if (Strings.isNullOrEmpty(mavenArtifactId)) {
      return DataflowProjectValidationStatus.NO_ARTIFACT_ID;
    }
    if (!MAVEN_ID_REGEX.matcher(mavenArtifactId).matches()) {
      return DataflowProjectValidationStatus.ILLEGAL_ARTIFACT_ID;
    }
    return DataflowProjectValidationStatus.OK;
  }

  private DataflowProjectValidationStatus validateMavenGroupId() {
    if (Strings.isNullOrEmpty(mavenGroupId)) {
      return DataflowProjectValidationStatus.NO_GROUP_ID;
    }
    if (!MAVEN_ID_REGEX.matcher(mavenGroupId).matches()) {
      return DataflowProjectValidationStatus.ILLEGAL_GROUP_ID;
    }
    return DataflowProjectValidationStatus.OK;
  }

  private DataflowProjectValidationStatus validatePackage() {
    if (Strings.isNullOrEmpty(packageString)
        || !JAVA_PACKAGE_REGEX.matcher(packageString).matches()) {
      return DataflowProjectValidationStatus.ILLEGAL_PACKAGE;
    }
    return DataflowProjectValidationStatus.OK;
  }

  private DataflowProjectValidationStatus validateProjectLocation() {
    if (!customLocation || projectLocation == null) {
      return DataflowProjectValidationStatus.OK;
    }

    File file = URIUtil.toFile(projectLocation);
    if (file == null) {
      return DataflowProjectValidationStatus.LOCATION_NOT_LOCAL;
    }
    if (!file.exists()) {
      return DataflowProjectValidationStatus.NO_SUCH_LOCATION;
    }
    if (!file.isDirectory()) {
      return DataflowProjectValidationStatus.LOCATION_NOT_DIRECTORY;
    }
    return DataflowProjectValidationStatus.OK;
  }

  private DataflowProjectValidationStatus validateProjectName() {
    if (Strings.isNullOrEmpty(projectNameTemplate)) {
      // The default names will be valid
      return DataflowProjectValidationStatus.OK;
    }

    IPath projectPath = new Path(null, projectNameTemplate).makeAbsolute();
    if (!projectPath.isValidSegment(projectNameTemplate)) {
      return DataflowProjectValidationStatus.PROJECT_NAME_NOT_SEGMENT;
    }
    return DataflowProjectValidationStatus.OK;
  }
}
