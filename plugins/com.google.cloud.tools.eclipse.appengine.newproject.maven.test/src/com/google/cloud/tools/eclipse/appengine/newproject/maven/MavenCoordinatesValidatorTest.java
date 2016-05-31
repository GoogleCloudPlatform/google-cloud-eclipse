
package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MavenCoordinatesValidatorTest {
  @Test
  public void testValidateArtifactId() {
    assertTrue(MavenCoordinatesValidator.validateArtifactId("foo"));
    assertTrue(MavenCoordinatesValidator.validateArtifactId("foo.bar"));
    assertTrue(MavenCoordinatesValidator.validateArtifactId("foo_bar"));
    assertFalse(MavenCoordinatesValidator.validateArtifactId(null));
    assertFalse(MavenCoordinatesValidator.validateArtifactId(""));
    assertFalse(MavenCoordinatesValidator.validateArtifactId("foo bar"));
  }

  @Test
  public void testValidateGroupId() {
    assertTrue(MavenCoordinatesValidator.validateGroupId("foo"));
    assertTrue(MavenCoordinatesValidator.validateGroupId("foo.bar"));
    assertTrue(MavenCoordinatesValidator.validateGroupId("foo_bar"));
    assertFalse(MavenCoordinatesValidator.validateGroupId(null));
    assertFalse(MavenCoordinatesValidator.validateGroupId(""));
    assertFalse(MavenCoordinatesValidator.validateGroupId("foo bar"));
  }

  @Test
  public void testValidateVersion() {
    assertTrue(MavenCoordinatesValidator.validateVersion("foo"));
    assertTrue(MavenCoordinatesValidator.validateVersion("foo.bar"));
    assertFalse(MavenCoordinatesValidator.validateVersion(null));
    assertFalse(MavenCoordinatesValidator.validateVersion(""));
  }
}
