package com.google.cloud.tools.eclipse.appengine.libraries;

import com.google.common.base.Preconditions;

/**
 * Describes a Maven artifact.
 */
public class MavenCoordinates {

  public static final String LATEST = "LATEST";
  private static final String JAR = "jar";

  private String repository;
  private String groupId;
  private String artifactId;
  private String version = LATEST;
  private String type = JAR;
  private String classifier;

  /**
   * @param repository the URI or the identifier of the repository used to download the artifact from. It is treated
   * as an URI if it starts with <code>&lt;protocol&gt;://</code>. Cannot be <code>null</code>.
   * @param groupId the Maven group ID, cannot be <code>null</code>
   * @param artifactId the Maven artifact ID, cannot be <code>null</code>
   */
  public MavenCoordinates(String repository, String groupId, String artifactId) {
    Preconditions.checkNotNull(repository, "repository null");
    Preconditions.checkNotNull(groupId, "groupId null");
    Preconditions.checkNotNull(artifactId, "artifactId null");
    Preconditions.checkArgument(!repository.isEmpty(), "repository empty");
    Preconditions.checkArgument(!groupId.isEmpty(), "groupId empty");
    Preconditions.checkArgument(!artifactId.isEmpty(), "artifactId empty");

    this.repository = repository;
    this.groupId = groupId;
    this.artifactId = artifactId;
  }

  /**
   * @return the Maven version of the artifact, defaults to special value {@link MavenCoordinates#LATEST}, never 
   * <code>null</code>
   */
  public String getVersion() {
    return version;
  }

  /**
   * @param version the Maven version of the artifact, defaults to special value {@link MavenCoordinates#LATEST},
   * cannot be set to <code>null</code> or empty string, if <code>null</code> or empty string is passed it, it is 
   * ignored and the version field remains unchanged.
   */
  public void setVersion(String version) {
    if (version != null && !version.isEmpty()) {
      this.version = version;
    }
  }

  /**
   * @return the Maven packaging type, defaults to <code>jar</code>, never <code>null</code>
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the Maven packaging type, defaults to <code>jar</code>, cannot be set to <code>null</code> or empty 
   * string, if <code>null</code> or empty string is passed it, it is ignored and the version field remains unchanged.
   */
  public void setType(String type) {
    if (type != null && !type.isEmpty()) {
      this.type = type;
    }
  }

  /**
   * @return the Maven classifier or <code>null</code> if it was not set
   */
  public String getClassifier() {
    return classifier;
  }

  /**
   * @param classifier the Maven classifier, defaults to null.
   */
  public void setClassifier(String classifier) {
    this.classifier = classifier;
  }

  /**
   * @return the URI or the identifier of the repository used to download the artifact from, never <code>null</code>
   */
  public String getRepository() {
    return repository;
  }

  /**
   * @return the Maven group ID, never <code>null</code>
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * @return the Maven artifact ID, never <code>null</code>
   */
  public String getArtifactId() {
    return artifactId;
  }
}
