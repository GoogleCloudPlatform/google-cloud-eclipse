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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.cloud.tools.eclipse.dataflow.core.DataflowCorePlugin;
import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowProjectCreator.Template;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableSortedSet.Builder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.NavigableSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * {@link DataflowArtifactRetriever} provides access to Maven artifacts for Dataflow projects, as
 * well as the examples source code.
 *
 * <p>The implementation of {@link DataflowArtifactRetriever} uses low-level URL and XPath APIs
 * rather than using the M2E plugin to work around shortcomings in the ability of M2E to query
 * Maven for available versions. Additionally, M2E APIs are internal and unstable, and thus may
 * change between versions.
 *
 * <p>The artifact retriever reads Maven Central metadata XML files to retrieve available and latest
 * versions.
 */
class DataflowArtifactRetriever {

  private static URL getMetadataUrl(String artifactId) {
    try {
      return new URL(
          String.format(
              "https://repo1.maven.org/maven2/com/google/cloud/dataflow/%s/maven-metadata.xml",
              artifactId));
    } catch (MalformedURLException e) {
      throw new IllegalStateException(
          String.format("Could not construct metadata URL for artifact %s", artifactId), e);
    }
  }

  private final LoadingCache<String, ArtifactVersion> latestVersion =
      CacheBuilder.newBuilder()
          .refreshAfterWrite(4, TimeUnit.HOURS)
          .build(
              new CacheLoader<String, ArtifactVersion>() {

                @Override
                public ArtifactVersion load(String artifactId) throws Exception {
                  XPath xpath = XPathFactory.newInstance().newXPath();
                  Document document = getMetadataDocument(artifactId);
                  String result = xpath.evaluate("/metadata/versioning/latest", document);
                  return new DefaultArtifactVersion(result);
                }
              });

  private final LoadingCache<String, NavigableSet<ArtifactVersion>> availableVersions =
      CacheBuilder.newBuilder()
          .refreshAfterWrite(4, TimeUnit.HOURS)
          .build(
              new CacheLoader<String, NavigableSet<ArtifactVersion>>() {

                @Override
                public NavigableSet<ArtifactVersion> load(String artifactId) throws Exception {
                  XPath xpath = XPathFactory.newInstance().newXPath();
                  Document document = getMetadataDocument(artifactId);
                  NodeList versionNodes = (NodeList) xpath.evaluate(
                    "/metadata/versioning/versions/version",
                    document,
                    XPathConstants.NODESET);
                  Builder<ArtifactVersion> versions = ImmutableSortedSet.naturalOrder();
                  for (int i = 0; i < versionNodes.getLength(); i++) {
                    String versionString = versionNodes.item(i).getTextContent();
                    versions.add(new DefaultArtifactVersion(versionString));
                  }
                  return versions.build();
                }
              });

  private static final DataflowArtifactRetriever INSTANCE = new DataflowArtifactRetriever();
  static DataflowArtifactRetriever defaultInstance() {
    return INSTANCE;
  }
  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Returns the latest published Archetype Version in the version range, or null if there is no
   * such version.
   */
  ArtifactVersion getLatestArchetypeVersion(Template template, MajorVersion majorVersion) {
    checkArgument(template.getSdkVersions().contains(majorVersion));
    return getLatestIncrementalVersion(template.getArchetype(), majorVersion.getVersionRange());
  }

  /**
   * Returns the latest Version of the specified Artifact in the version range, or null if there is
   * no such version.
   */
  private ArtifactVersion getLatestIncrementalVersion(
      String artifactId, VersionRange versionRange) {
    try {
      ArtifactVersion latest = latestVersion.get(artifactId);
      if (versionRange.containsVersion(latest)) {
        return latest;
      }
    } catch (ExecutionException e) {
      DataflowCorePlugin.logWarning(
          e, "Could not retrieve latest version for Artifact %s", artifactId);
    }

    try {
      NavigableSet<ArtifactVersion> allVersions = availableVersions.get(artifactId);
      ArtifactVersion latest = getLatestInRange(versionRange, allVersions);
      if (latest != null) {
        return latest;
      }
    } catch (ExecutionException e) {
      DataflowCorePlugin.logWarning(
          e, "Could not retrieve available versions for Artifact %s", artifactId);
    }
    return getLatestInRange(versionRange, DataflowMavenCoordinates.KNOWN_VERSIONS);
  }

  private ArtifactVersion getLatestInRange(
      VersionRange versionRange, NavigableSet<ArtifactVersion> allVersions) {
    for (ArtifactVersion version : allVersions.descendingSet()) {
      if (versionRange.containsVersion(version)) {
        return version;
      }
    }
    return null;
  }

  private Document getMetadataDocument(String artifactId) throws CoreException {
    try {
      return DocumentBuilderFactory.newInstance()
          .newDocumentBuilder()
          .parse(getMetadataUrl(artifactId).toString());
    } catch (IOException e) {
      throw new CoreException(
          new Status(
              Status.ERROR,
              DataflowCorePlugin.PLUGIN_ID,
              "Could not retrieve Dataflow SDK Metadata",
              e));
    } catch (ParserConfigurationException | SAXException e) {
      throw new CoreException(
          new Status(
              Status.ERROR,
              DataflowCorePlugin.PLUGIN_ID,
              "Could not configure Document Builder",
              e));
    }
  }
}
