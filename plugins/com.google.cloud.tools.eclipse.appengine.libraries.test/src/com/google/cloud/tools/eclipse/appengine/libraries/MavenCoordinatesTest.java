package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MavenCoordinatesTest {

  @Test(expected = NullPointerException.class)
  public void testConstructorRepositoryNull() {
    new MavenCoordinates(null, "a", "b");
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorGroupIdNull() {
    new MavenCoordinates("a", null, "b");
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorArtifactIdNull() {
    new MavenCoordinates("a", "b", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorEmptyRepository() {
    new MavenCoordinates("", "a", "b");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorEmptyGroupId() {
    new MavenCoordinates("a", "", "b");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorEmptyArtifactId() {
    new MavenCoordinates("a", "b", "");
  }

  @Test
  public void testConstructorValidArguments() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    assertThat(mavenCoordinates.getRepository(), is("a"));
    assertThat(mavenCoordinates.getGroupId(), is("b"));
    assertThat(mavenCoordinates.getArtifactId(), is("c"));
  }

  @Test
  public void testVersionDefaultsToLatest() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    assertThat(mavenCoordinates.getVersion(), is(MavenCoordinates.LATEST));
  }

  @Test(expected = NullPointerException.class)
  public void testSetNullVersion() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    mavenCoordinates.setVersion(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetEmptyVersion() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    mavenCoordinates.setVersion("");
  }

  @Test
  public void setVersion() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    mavenCoordinates.setVersion("1");
    assertThat(mavenCoordinates.getVersion(), is("1"));
  }

  @Test
  public void testTypeDefaultsToJar() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    assertThat(mavenCoordinates.getType(), is("jar"));
  }

  @Test(expected = NullPointerException.class)
  public void testSetNullType() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    mavenCoordinates.setType(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetEmptyType() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    mavenCoordinates.setType("");
  }

  @Test
  public void testSetType() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    mavenCoordinates.setType("war");
    assertThat(mavenCoordinates.getType(), is("war"));
  }

  @Test
  public void testClassifierDefaultsToNull() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    assertNull(mavenCoordinates.getClassifier());
  }

  @Test
  public void testSetClassifier() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    mavenCoordinates.setClassifier("d");
    assertThat(mavenCoordinates.getClassifier(), is("d"));
  }

  @Test
  public void testSetNullClassifier() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    mavenCoordinates.setClassifier(null);
    assertNull(mavenCoordinates.getClassifier());
  }

  @Test
  public void testSetEmptyClassifier() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    mavenCoordinates.setClassifier("");
    assertThat(mavenCoordinates.getClassifier(), is(""));
  }
}
