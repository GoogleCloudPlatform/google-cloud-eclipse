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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * Tests for {@link DataflowArtifactRetriever}.
 */
@RunWith(JUnit4.class)
public class DataflowDependencyManagerTest {
  @Mock
  private DataflowArtifactRetriever artifactRetriever;

  private DataflowDependencyManager manager;

  @Mock private IMaven maven;
  @Mock private IMavenProjectRegistry projectRegistry;
  @Mock private IProject project;
  @Mock private IFile pomFile;
  @Mock private InputStream pomInputStream;
  @Mock private IMavenProjectFacade mavenFacade;
  @Mock private Model model;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    manager = DataflowDependencyManager.create(artifactRetriever, maven, projectRegistry);

    when(projectRegistry.getProject(project)).thenReturn(mavenFacade);
    when(mavenFacade.getPom()).thenReturn(pomFile);
    when(pomFile.getContents()).thenReturn(pomInputStream);
    when(maven.readModel(pomInputStream)).thenReturn(model);
  }

  @Test
  public void testGetDataflowDependencyNoTrackDependsUpToNextMajorVersion() throws Exception {
    ArtifactVersion baseVersion = new DefaultArtifactVersion("1.2.3");
    when(artifactRetriever.getLatestSdkVersion(MajorVersion.ONE.getVersionRange()))
        .thenReturn(baseVersion);

    ArtifactVersion version =
        manager.getLatestDataflowDependencyInRange(MajorVersion.ONE.getVersionRange());

    assertEquals(baseVersion, version);
  }

  @Test
  public void testGetDataflowDependencyNoTrackNoVersionInRangeDependsOnUnstableMajorVersionRange()
      throws Exception {
    when(artifactRetriever.getLatestSdkVersion(MajorVersion.QUALIFIED_TWO.getVersionRange()))
        .thenReturn(null);

    ArtifactVersion version =
        manager.getLatestDataflowDependencyInRange(MajorVersion.QUALIFIED_TWO.getVersionRange());

    assertNull(version);
  }

  @Test
  public void testGetDataflowDependencyNoTrackNoVersionInRangeDependsOnMajorVersionRange()
      throws Exception {
    when(artifactRetriever.getLatestSdkVersion(MajorVersion.TWO.getVersionRange()))
        .thenReturn(null);

    ArtifactVersion version =
        manager.getLatestDataflowDependencyInRange(MajorVersion.TWO.getVersionRange());

    assertNull(version);
  }

  @Test
  public void testGetProjectMajorVersion() throws InvalidVersionSpecificationException {
    Dependency pinnedDep = pinnedDataflowDependency();
    when(model.getDependencies()).thenReturn(ImmutableList.of(pinnedDep));
    DefaultArtifactVersion latestVersion = new DefaultArtifactVersion("1.2.3");
    when(
        artifactRetriever.getLatestSdkVersion(
            VersionRange.createFromVersionSpec(pinnedDep.getVersion()))).thenReturn(latestVersion);

    assertEquals(MajorVersion.ONE, manager.getProjectMajorVersion(project));
  }

  @Test
  public void testGetLatestVersions() {
    DefaultArtifactVersion latestVersionOne = new DefaultArtifactVersion("1.2.3");
    when(artifactRetriever.getLatestSdkVersion(MajorVersion.ONE.getVersionRange()))
        .thenReturn(latestVersionOne);
    ArtifactVersion latestVersionTwo = new DefaultArtifactVersion("2.0.0");
    when(artifactRetriever.getLatestSdkVersion(MajorVersion.TWO.getVersionRange()))
        .thenReturn(latestVersionTwo);

    Map<ArtifactVersion, MajorVersion> expected =
        ImmutableMap.<ArtifactVersion, MajorVersion>builder()
            .put(latestVersionOne, MajorVersion.ONE)
            .put(latestVersionTwo, MajorVersion.TWO)
            .build();
    assertEquals(
        expected,
        manager.getLatestVersions(ImmutableSortedSet.of(MajorVersion.ONE, MajorVersion.TWO)));
  }

  @Test
  public void testGetLatestVersionUnstableNoReleasedStableVersion() {
    DefaultArtifactVersion latestVersion = new DefaultArtifactVersion("2.0.0-beta2");
    when(artifactRetriever.getLatestSdkVersion(MajorVersion.QUALIFIED_TWO.getVersionRange()))
        .thenReturn(latestVersion);
    when(artifactRetriever.getLatestSdkVersion(MajorVersion.TWO.getVersionRange()))
        .thenReturn(null);

    assertEquals(
        Collections.singletonMap(latestVersion, MajorVersion.QUALIFIED_TWO),
        manager.getLatestVersions(
            ImmutableSortedSet.of(MajorVersion.QUALIFIED_TWO, MajorVersion.TWO)));
  }

  @Test
  public void testGetLatestVersionUnstableWithStableVersionInMap() {
    DefaultArtifactVersion latestQualified = new DefaultArtifactVersion("2.0.0-beta2");
    when(artifactRetriever.getLatestSdkVersion(MajorVersion.QUALIFIED_TWO.getVersionRange()))
        .thenReturn(latestQualified);
    DefaultArtifactVersion latestMajor = new DefaultArtifactVersion("2.0.0");
    when(artifactRetriever.getLatestSdkVersion(MajorVersion.TWO.getVersionRange()))
        .thenReturn(latestMajor);

    assertEquals(
        Collections.singletonMap(latestMajor, MajorVersion.TWO),
        manager.getLatestVersions(
            ImmutableSortedSet.of(MajorVersion.QUALIFIED_TWO, MajorVersion.TWO)));
  }

  @Test
  public void testGetLatestVersionUnstableWithNoStableVersion() {
    DefaultArtifactVersion latestOne = new DefaultArtifactVersion("1.9.0");
    when(artifactRetriever.getLatestSdkVersion(MajorVersion.ONE.getVersionRange()))
        .thenReturn(latestOne);
    DefaultArtifactVersion latestQualifiedTwo = new DefaultArtifactVersion("2.0.0-beta2");
    when(artifactRetriever.getLatestSdkVersion(MajorVersion.QUALIFIED_TWO.getVersionRange()))
        .thenReturn(latestQualifiedTwo);

    Map<ArtifactVersion, MajorVersion> expected =
        ImmutableMap.<ArtifactVersion, MajorVersion>builder()
            .put(latestOne, MajorVersion.ONE)
            .put(latestQualifiedTwo, MajorVersion.QUALIFIED_TWO)
            .build();
    assertEquals(
        expected,
        manager.getLatestVersions(
            ImmutableSortedSet.of(MajorVersion.ONE, MajorVersion.QUALIFIED_TWO)));
  }

  @Test
  public void hasTrackedDependencyNoDependency() {
    when(model.getDependencies()).thenReturn(ImmutableList.<Dependency>of());

    assertFalse(manager.hasTrackedDataflowDependency(project));
  }

  @Test
  public void hasTrackedDependencyPinnedDependency() {
    when(model.getDependencies())
        .thenReturn(ImmutableList.<Dependency>of(pinnedDataflowDependency()));

    assertFalse(manager.hasTrackedDataflowDependency(project));
  }

  @Test
  public void hasTrackedDependencyTrackedDependency() {
    when(model.getDependencies())
        .thenReturn(ImmutableList.<Dependency>of(trackedDataflowDependency()));

    assertTrue(manager.hasTrackedDataflowDependency(project));
  }

  @Test
  public void hasPinnedDependencyNoDependency() {
    when(model.getDependencies()).thenReturn(ImmutableList.<Dependency>of());

    assertFalse(manager.hasPinnedDataflowDependency(project));
  }

  @Test
  public void hasPinnedDependencyPinnedDependency() {
    when(model.getDependencies())
        .thenReturn(ImmutableList.<Dependency>of(pinnedDataflowDependency()));

    assertTrue(manager.hasPinnedDataflowDependency(project));
  }

  @Test
  public void hasPinnedDependencyTrackedDependency() {
    when(model.getDependencies())
        .thenReturn(ImmutableList.<Dependency>of(trackedDataflowDependency()));

    assertFalse(manager.hasPinnedDataflowDependency(project));
  }

  private Dependency dataflowDependency() {
    Dependency dep = new Dependency();
    dep.setArtifactId("google-cloud-dataflow-java-sdk-all");
    dep.setGroupId("com.google.cloud.dataflow");
    return dep;
  }

  private Dependency pinnedDataflowDependency() {
    Dependency dep = dataflowDependency();
    dep.setVersion("[1.0.0, 2.0.0)");
    return dep;
  }

  private Dependency trackedDataflowDependency() {
    Dependency dep = dataflowDependency();
    dep.setVersion(Artifact.LATEST_VERSION);
    return dep;
  }
}
