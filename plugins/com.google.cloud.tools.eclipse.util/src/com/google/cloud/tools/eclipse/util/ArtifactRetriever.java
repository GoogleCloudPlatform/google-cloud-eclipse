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
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableSortedSet.Builder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
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
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * {@link ArtifactRetriever} provides access to Maven artifacts using low-level URL and XPath APIs
 * rather than using the M2E plugin in order to work around shortcomings in the ability of M2E to
 * query Maven for available versions. Additionally, M2E APIs are internal and unstable, and thus
 * may change between versions.
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

  private final LoadingCache<String, NavigableSet<ArtifactVersion>> availableVersions =
      CacheBuilder.newBuilder()
          .refreshAfterWrite(4, TimeUnit.HOURS)
          .build(
              new CacheLoader<String, NavigableSet<ArtifactVersion>>() {

                @Override
                public NavigableSet<ArtifactVersion> load(String coordinates) throws Exception {
                  Document document = getMetadataDocument(coordinates);
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

  private static final LoadingCache<String, ArtifactRetriever> retrievers =
      CacheBuilder.newBuilder()
          .build(
              new CacheLoader<String, ArtifactRetriever>() {

                @Override
                public ArtifactRetriever load(String url) {
                  return new ArtifactRetriever(url);
                }
              });

  /**
   * A retriever attached to Maven Central https://repo1.maven.org/maven2/
   */
  public static final ArtifactRetriever DEFAULT = central();

  /**
   * Avoid some exception catching during initialization of
   * the known valid Maven Central URL.
   **/
  private static ArtifactRetriever central() {
    return retrievers.getUnchecked("https://repo1.maven.org/maven2/");
  }

  /**
   * @param repositoryUrl the base URL of the maven mirror such as
   *     "https://repo1.maven.org/maven2/"
   */
  private ArtifactRetriever(String repositoryUrl) {
    this.repositoryUrl = repositoryUrl;
  }

  /**
   * Returns the latest published release artifact version, or null if there is no such version.
   */
  public ArtifactVersion getLatestReleaseVersion(String groupId, String artifactId) {
    return getLatestReleaseVersion(groupId, artifactId, null);
  }

  /**
   * Returns the latest published release artifact version in the version range,
   * or null if there is no such version.
   */
  public ArtifactVersion getLatestReleaseVersion(
      String groupId, String artifactId, VersionRange range) {
    String coordinates = idToKey(groupId, artifactId);
    try {
      NavigableSet<ArtifactVersion> versions = availableVersions.get(coordinates);
      for (ArtifactVersion version : versions.descendingSet()) {
        if (isReleased(version)) {
          if (range == null || range.containsVersion(version)) {
            return version;
          }
        }
      }
    } catch (ExecutionException ex) {
      logger.log(
          Level.WARNING,
          "Could not retrieve version for artifact " + coordinates,
          ex.getCause());
    }
    return null;
  }

  private static boolean isReleased(ArtifactVersion version) {
    String qualifier = version.getQualifier();
    if (version.getMajorVersion() <= 0) {
      return false;
    } else if (Strings.isNullOrEmpty(qualifier)) {
      return true; 
    } else if ("final".equalsIgnoreCase(qualifier.toLowerCase(Locale.US))) {
      return true; 
    }
    
    return false;
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

  /**
   * Returns a possibly cached instance of a retriever connected to a particular repo.
   *
   * @param url the URL of the repository to retrieve from
   * @throws URISyntaxException if the argument is not a valid URL
   */
  public static ArtifactRetriever getInstance(String url) throws URISyntaxException {
    Preconditions.checkNotNull(url);
    // check for URL syntax
    new URI(url);

    if (!url.endsWith("/")) {
      url = url + "/";
    }
    
    return retrievers.getUnchecked(url);
  }
}
