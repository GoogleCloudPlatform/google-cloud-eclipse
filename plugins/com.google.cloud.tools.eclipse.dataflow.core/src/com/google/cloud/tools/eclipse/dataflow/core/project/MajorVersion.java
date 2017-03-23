/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.google.cloud.tools.eclipse.dataflow.core.project;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Strings;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

/** A Dataflow Major Version. */
public enum MajorVersion {
  ONE {
    @Override
    public ArtifactVersion getInitialVersion() {
      return new DefaultArtifactVersion("1.0.0");
    }

    @Override
    public ArtifactVersion getMaxVersion() {
      return new DefaultArtifactVersion("1.99.0");
    }

    @Override
    public MajorVersion getStableVersion() {
      return this;
    }
  },
  QUALIFIED_TWO {
    @Override
    public ArtifactVersion getInitialVersion() {
      return new DefaultArtifactVersion("2.0.0-beta1");
    }
    @Override
    public ArtifactVersion getMaxVersion() {
      return new DefaultArtifactVersion("2.0.0");
    }

    /**
     * Qualified Versions do not have compatibility guarantees. e.g. {@code 2.0.0-beta1} and
     * {@code 2.0.0-beta2} have no guarantees, even though they share major, minor, and incremental
     * versions.
     */
    @Override
    public boolean hasStableApi() {
      return false;
    }

    public MajorVersion getStableVersion() {
      return TWO;
    }
  },
  TWO {
    @Override
    public ArtifactVersion getInitialVersion() {
      return new DefaultArtifactVersion("2.0.0");
    }
    @Override
    public ArtifactVersion getMaxVersion() {
      return new DefaultArtifactVersion("2.99.0");
    }

    @Override
    public MajorVersion getStableVersion() {
      return this;
    }
  },
  THREE_PLUS {
    @Override
    public ArtifactVersion getInitialVersion() {
      // Generally this specific value should only refer to the `THREE_PLUS` value for a short time
      // before the plugin is updated and this becomes THREE and FOUR_PLUS is introduced.
      return new DefaultArtifactVersion("3.0.0");
    }
    @Override
    public ArtifactVersion getMaxVersion() {
      return new DefaultArtifactVersion("");
    }

    @Override
    public boolean hasStableApi() {
      return false;
    }

    @Override
    public MajorVersion getStableVersion() {
      throw new IllegalArgumentException(this + " can not have an associated stable version");
    }
  },
  ALL {
    @Override
    public ArtifactVersion getInitialVersion() {
      return new DefaultArtifactVersion("0.0.0");
    }

    @Override
    public ArtifactVersion getMaxVersion() {
      return new DefaultArtifactVersion("");
    }

    /**
     * Versions within the version range {@link #ALL} can be between any number of ranges.
     */
    @Override
    public boolean hasStableApi() {
      return false;
    }

    @Override
    public MajorVersion getStableVersion() {
      throw new IllegalArgumentException(this + " can not have an associated stable version");
    }
  };

  public abstract ArtifactVersion getInitialVersion();
  public abstract ArtifactVersion getMaxVersion();

  private String getVersionSpec() {
    return String.format("[%s, %s)", getInitialVersion(), getMaxVersion());
  }

  public VersionRange getVersionRange() {
    String spec = getVersionSpec();
    try {
      return VersionRange.createFromVersionSpec(spec);
    } catch (InvalidVersionSpecificationException e) {
      throw new IllegalStateException("Could not create the constant version range " + spec, e);
    }
  }

  /**
   * Returns if the version range has a stable API.
   */
  public boolean hasStableApi() {
    return true;
  }

  /**
   * Returns the nearest future {@link MajorVersion} with a stable API. For
   * {@link MajorVersion MajorVersions} with a stable API, returns this {@link MajorVersion}. If
   * there is no such version, throws an {@link IllegalArgumentException}.
   */
  public abstract MajorVersion getStableVersion();

  public VersionRange getTruncatedVersionRange(ArtifactVersion latestVersion) {
    return truncateAtLatest(latestVersion, getVersionRange());
  }

  /**
   * Returns the version range that begins at the provided version and ends at the maximum version
   * of the provided version range. If not,
   *
   * <p>If the {@link MajorVersion} of the {@code latestVersion} does not have a stable API (as
   * defined by {@link #hasStableApi()}, this will truncate the version range
   * at both ends, to ensure the available versions are not incompatible with the current version.
   *
   * @param latestVersion The minimum version of the returned version range
   * @param currentVersionRange A version range that ends at the maximum version of the returned
   *                            version range
   */
  public static VersionRange truncateAtLatest(
      ArtifactVersion latestVersion, VersionRange currentVersionRange) {
    checkArgument(
        currentVersionRange.containsVersion(latestVersion),
        "Tried to truncate a version range %s at a version %s it does not contain",
        currentVersionRange,
        latestVersion);
    try {
      if (fromVersion(latestVersion).hasStableApi()) {
        VersionRange atLeastLatestVersion =
            VersionRange.createFromVersionSpec(String.format("[%s,)", latestVersion.toString()));
        return currentVersionRange.restrict(atLeastLatestVersion);
      }
      return VersionRange.createFromVersion(latestVersion.toString());
    } catch (InvalidVersionSpecificationException e) {
      throw new IllegalArgumentException(
          String.format("Cannot create a Version Range starting at %s", latestVersion), e);
    }
  }

  public static MajorVersion fromVersion(ArtifactVersion version) {
    switch (version.getMajorVersion()) {
      case 0:
        return ALL;
      case 1:
        return ONE;
      case 2:
        if (Strings.isNullOrEmpty(version.getQualifier())) {
          return TWO;
        } else {
          return QUALIFIED_TWO;
        }
      default:
        return THREE_PLUS;
    }
  }
}
