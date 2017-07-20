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

package com.google.cloud.tools.eclipse.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableSortedSet.Builder;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.NavigableSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * {@link ArtifactRetriever} provides access to Maven artifacts using low-level URL and XPath APIs
 * rather than using the M2E plugin to work around shortcomings in the ability of M2E to query
 * Maven for available versions. Additionally, M2E APIs are internal and unstable, and thus may
 * change between versions.
 *
 * <p>The artifact retriever reads Maven Central metadata XML files to retrieve available and latest
 * versions.
 */
public class ArtifactRetriever {
  
  private static final Logger logger = Logger.getLogger(ArtifactRetriever.class.getName());
  
  private final String repositoryUrl;
  
  @VisibleForTesting
  URL getMetadataUrl(String groupId, String artifactId) {
    String groupPath = groupId.replace('.', '/');
    try {
      return new URL(
          repositoryUrl + groupPath + "/" + artifactId + "/maven-metadata.xml");
    } catch (MalformedURLException ex) {
      throw new IllegalStateException(
          "Could not construct metadata URL for artifact " + artifactId, ex);
    }
  }
  
  private final LoadingCache<String, Document> metadataCache =
      CacheBuilder.newBuilder()
          .refreshAfterWrite(4, TimeUnit.HOURS)
          .build(
              new CacheLoader<String, Document>() {

                @Override
                public Document load(String coordinates) throws Exception {
                  return getMetadataDocument(coordinates);
                }
              });

  private final LoadingCache<String, ArtifactVersion> latestVersion =
      CacheBuilder.newBuilder()
          .refreshAfterWrite(4, TimeUnit.HOURS)
          .build(
              new CacheLoader<String, ArtifactVersion>() {

                @Override
                public ArtifactVersion load(String coordinates) throws Exception {
                  Document document = metadataCache.get(coordinates);
                  XPath xpath = XPathFactory.newInstance().newXPath();
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
                public NavigableSet<ArtifactVersion> load(String coordinates) throws Exception {
                  Document document = metadataCache.get(coordinates);
                  XPath xpath = XPathFactory.newInstance().newXPath();
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

  /**
   * @param repositoryUrl the base URL of the maven mirror such as 
   *     "https://repo1.maven.org/maven2/"
   * @throws URISyntaxException if the argument is not a valid URL
   */
  public ArtifactRetriever(String repositoryUrl) throws URISyntaxException {
    Preconditions.checkNotNull(repositoryUrl);
    // check for URL syntax
    new URI(repositoryUrl);
    if (!repositoryUrl.endsWith("/")) {
      repositoryUrl = repositoryUrl + "/";
    }
    
    this.repositoryUrl = repositoryUrl;
  }

  /**
   * Retrieve from https://repo1.maven.org/maven2/
   */
  public ArtifactRetriever() {
    this.repositoryUrl = "https://repo1.maven.org/maven2/";
  }

  /**
   * Returns the latest published artifact version, or null if there is no such version.
   */
  public ArtifactVersion getLatestArtifactVersion(String groupId, String artifactId) {
    return getLatestIncrementalVersion(idToKey(groupId, artifactId), null);
  }

  /**
   * Returns the latest published artifact version in the version range, or null if there is no such
   * version.
   */
  public ArtifactVersion getLatestArtifactVersion(
      String groupId, String artifactId, VersionRange range) {
    return getLatestIncrementalVersion(idToKey(groupId, artifactId), range);
  }

  /**
   * Returns the latest version of the specified artifact in the version range, or null if there is
   * no such version.
   * 
   * @param coordinates Maven coordinates in the form groupId:artifactId
   */
  private ArtifactVersion getLatestIncrementalVersion(String coordinates, VersionRange range) {
    try {
      ArtifactVersion latest = latestVersion.get(coordinates);
      if (range == null || range.containsVersion(latest)) {
        return latest;
      }
    } catch (ExecutionException ex) {
      logger.log(
          Level.WARNING,
          "Could not retrieve latest version for artifact " + coordinates,
          ex.getCause());
    }

    try {
      NavigableSet<ArtifactVersion> allVersions = availableVersions.get(coordinates);
      ArtifactVersion latest = getLatestInRange(range, allVersions);
      return latest;
    } catch (ExecutionException ex) {
      logger.log(
          Level.WARNING,
          "Could not retrieve available versions for artifact " + coordinates,
          ex.getCause());
      return null;
    }
  }

  private static ArtifactVersion getLatestInRange(
      VersionRange versionRange, NavigableSet<ArtifactVersion> allVersions) {
    for (ArtifactVersion version : allVersions.descendingSet()) {
      if (versionRange.containsVersion(version)) {
        return version;
      }
    }
    return null;
  }

  private Document getMetadataDocument(String coordinates) throws IOException {
    String[] x = keyToId(coordinates);
    String groupId = x[0];
    String artifactId = x[1];
    try {
      return DocumentBuilderFactory.newInstance()
          .newDocumentBuilder()
          .parse(getMetadataUrl(groupId, artifactId).toString());
    } catch (ParserConfigurationException | SAXException ex) {
      // these really shouldn't happen but if they do we'll wrap them
      throw new IOException("Could not configure Document Builder", ex);
    }
  }

  @VisibleForTesting
  static String idToKey(String groupId, String artifactId) {
    return groupId + ":" + artifactId;
  }

  @VisibleForTesting
  static String[] keyToId(String coordinates) {
    return coordinates.split(":");
  }
}
