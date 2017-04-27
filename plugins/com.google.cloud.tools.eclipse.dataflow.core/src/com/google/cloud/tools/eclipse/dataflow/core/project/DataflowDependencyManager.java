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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

/**
 * {@link DataflowDependencyManager} provides {@code Dependency} instances for Dataflow and the
 * Dataflow Examples.
 */
public class DataflowDependencyManager {
  private final DataflowArtifactRetriever artifactRetriever;

  private final IMaven maven;
  private final IMavenProjectRegistry mavenProjectRegistry;

  public static DataflowDependencyManager create() {
    return create(
        DataflowArtifactRetriever.defaultInstance(),
        MavenPlugin.getMaven(),
        MavenPlugin.getMavenProjectRegistry());
  }

  @VisibleForTesting
  static DataflowDependencyManager create(
      DataflowArtifactRetriever artifactRetriever,
      IMaven maven,
      IMavenProjectRegistry mavenProjectRegistry) {
    return new DataflowDependencyManager(artifactRetriever, maven, mavenProjectRegistry);
  }

  private DataflowDependencyManager(
      DataflowArtifactRetriever artifactRetriever,
      IMaven maven,
      IMavenProjectRegistry mavenProjectRegistry) {
    this.artifactRetriever = artifactRetriever;
    this.maven = maven;
    this.mavenProjectRegistry = mavenProjectRegistry;
  }

  /**
   * Retrieves a dependency on the Dataflow Java SDK. If trackUpdates is true, the version is
   * LATEST. Otherwise, the version is [Current Version, Next Major Version).
   */
  public ArtifactVersion getLatestDataflowDependencyInRange(VersionRange currentVersionRange) {
    return artifactRetriever.getLatestSdkVersion(currentVersionRange);
  }

  private static boolean isDataflowDependency(Dependency dependency) {
    return dependency.getGroupId().equals(DataflowArtifactRetriever.DATAFLOW_GROUP_ID)
        && dependency.getArtifactId().equals(DataflowArtifactRetriever.DATAFLOW_SDK_ARTIFACT);
  }

  /**
   * Returns {@code true} if the provided {@code Model} has a dependency on the Dataflow Java SDK
   * with a version other than LATEST or RELEASE.
   */
  public boolean hasPinnedDataflowDependency(IProject project) {
    Model model = getModelFromProject(project);
    if (model == null) {
      return false;
    }
    Dependency dependency = getDataflowDependencyFromModel(model);
    if (dependency == null
        || Artifact.LATEST_VERSION.equals(dependency.getVersion())
        || Artifact.RELEASE_VERSION.equals(dependency.getVersion())) {
      return false;
    }
    return true;
  }

  /**
   * Returns {@code true} if the provided {@code Model} has a dependency on the Dataflow Java SDK
   * with version LATEST or RELEASE.
   */
  public boolean hasTrackedDataflowDependency(IProject project) {
    Model model = getModelFromProject(project);
    if (model == null) {
      return false;
    }
    Dependency dependency = getDataflowDependencyFromModel(model);
    if (dependency == null) {
      return false;
    }
    return (Artifact.LATEST_VERSION.equals(dependency.getVersion())
        || Artifact.RELEASE_VERSION.equals(dependency.getVersion()));
  }

  public VersionRange getDataflowVersionRange(IProject project) {
    Dependency dfDependency =
        getDataflowDependencyFromModel(getModelFromProject(project));
    if (dfDependency != null) {
      String version = dfDependency.getVersion();
      if (Strings.isNullOrEmpty(version)) {
        return allVersions();
      } else {
        try {
          return VersionRange.createFromVersionSpec(version);
        } catch (InvalidVersionSpecificationException e) {
          throw new IllegalStateException(
              String.format(
                  "Could not create version range from existing version %s", version),
              e);
        }
      }
    } else {
      return allVersions();
    }
  }

  private VersionRange allVersions() {
    try {
      return VersionRange.createFromVersionSpec("[1.0.0,)");
    } catch (InvalidVersionSpecificationException e) {
      throw new IllegalStateException(
          "Could not create constant version Range [1.0.0,)", e);
    }
  }

  public MajorVersion getProjectMajorVersion(IProject project) {
    VersionRange projectVersionRange = getDataflowVersionRange(project);
    if (projectVersionRange.getRecommendedVersion() != null) {
      return MajorVersion.fromVersion(projectVersionRange.getRecommendedVersion());
    } else {
      ArtifactVersion dataflowVersion = getLatestDataflowDependencyInRange(projectVersionRange);
      return MajorVersion.fromVersion(dataflowVersion);
    }
  }

  private Model getModelFromProject(IProject project) {
    IMavenProjectFacade facade = mavenProjectRegistry.getProject(project);
    if (facade != null) {
      IFile pom = facade.getPom();
      try {
        return maven.readModel(pom.getContents());
      } catch (CoreException e) {
        return null;
      }
    }
    return null;
  }

  private static Dependency getDataflowDependencyFromModel(Model model) {
    for (Dependency dependency : model.getDependencies()) {
      if (isDataflowDependency(dependency)) {
        return dependency;
      }
    }
    return null;
  }

  /**
   * Retrieves the latest version for each provided major version if it is available.
   *
   * <p>For each provided version, if there is an available version within the major version range
   * and {@link MajorVersion#hasStableApi()} returns true, or
   * {@link MajorVersion#getStableVersion()} is not in the list of requested versions, that version
   * will appear in the returned map. Otherwise, if the {@link MajorVersion#hasStableApi()} returns
   * false and there is no available version for {@link MajorVersion#getStableVersion()}, the latest
   * version in the Unstable version range will be returned.
   */
  public NavigableMap<ArtifactVersion, MajorVersion> getLatestVersions(
      NavigableSet<MajorVersion> majorVersions) {
    NavigableMap<ArtifactVersion, MajorVersion> result = new TreeMap<>();
    Table<MajorVersion, ArtifactVersion, MajorVersion> unstableVersions = HashBasedTable.create();
    for (MajorVersion majorVersion : majorVersions) {
      ArtifactVersion latestInMajorVersion =
          getLatestDataflowDependencyInRange(majorVersion.getVersionRange());
      if (latestInMajorVersion != null) {
        if (majorVersion.hasStableApi()
            || !majorVersions.contains(majorVersion.getStableVersion())) {
          result.put(latestInMajorVersion, majorVersion);
        } else {
          unstableVersions.put(majorVersion.getStableVersion(), latestInMajorVersion, majorVersion);
        }
      } else {
        // All Major Versions that are unstable and have an associated stable version precede the
        // stable major version with the natural ordering, so we will never get a stable version
        // before the unstable versions
        for (Map.Entry<ArtifactVersion, MajorVersion> unstableVersion :
            unstableVersions.row(majorVersion).entrySet()) {
          result.put(unstableVersion.getKey(), unstableVersion.getValue());
        }
      }
    }
    if (result.isEmpty()) {
      result.put(new DefaultArtifactVersion("LATEST"), MajorVersion.ALL);
    }
    return result;
  }
}
