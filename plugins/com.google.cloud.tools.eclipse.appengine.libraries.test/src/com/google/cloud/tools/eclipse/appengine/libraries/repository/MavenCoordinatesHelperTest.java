/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import java.text.MessageFormat;
import java.util.List;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.junit.Before;
import org.junit.Test;

public class MavenCoordinatesHelperTest {

  private MavenCoordinates mavenCoordinates;
  
  @Before
  public void setUp() throws Exception {
    mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setRepository("testRepo");
    mavenCoordinates.setType("war");
    mavenCoordinates.setVersion(MavenCoordinates.LATEST_VERSION);
  }

  @Test
  public void testCreateClasspathAttributesWithClassifier() {
    mavenCoordinates.setClassifier("classifier");

    List<IClasspathAttribute> classpathAttributes =
        MavenCoordinatesHelper.createClasspathAttributes(mavenCoordinates, "1.0.0");
    assertAttribute(classpathAttributes,
                    "com.google.cloud.tools.eclipse.appengine.libraries.repository", "testRepo");
    assertAttribute(classpathAttributes,
                    "com.google.cloud.tools.eclipse.appengine.libraries.groupid", "groupId");
    assertAttribute(classpathAttributes,
                    "com.google.cloud.tools.eclipse.appengine.libraries.artifactId", "artifactId");
    assertAttribute(classpathAttributes,
                    "com.google.cloud.tools.eclipse.appengine.libraries.type", "war");
    assertAttribute(classpathAttributes,
                    "com.google.cloud.tools.eclipse.appengine.libraries.version", "1.0.0");
    assertAttribute(classpathAttributes,
                    "com.google.cloud.tools.eclipse.appengine.libraries.classifier", "classifier");
  }

  @Test
  public void testCreateClasspathAttributesWithoutClassifier() {
    List<IClasspathAttribute> classpathAttributes =
        MavenCoordinatesHelper.createClasspathAttributes(mavenCoordinates, "1.0.0");
    for (IClasspathAttribute iClasspathAttribute : classpathAttributes) {
      assertThat(iClasspathAttribute.getName(),
                 not("com.google.cloud.tools.eclipse.appengine.libraries.classifier"));
    }
  }

  @Test(expected = NullPointerException.class)
  public void testCreateMavenCoordinates_nullArray() {
    MavenCoordinatesHelper.createMavenCoordinates(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateMavenCoordinates_emptyArray() {
    MavenCoordinatesHelper.createMavenCoordinates(new IClasspathAttribute[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateMavenCoordinates_artifactIdMissing() {
    IClasspathAttribute[] attributes = 
        new IClasspathAttribute[] { 
            createAttribute("com.google.cloud.tools.eclipse.appengine.libraries.groupid", "groupId")
    };
    MavenCoordinatesHelper.createMavenCoordinates(attributes);
  }

  @Test
  public void testCreateMavenCoordinates_withGroupAndArtifactId() {
    IClasspathAttribute[] attributes = 
        new IClasspathAttribute[] { 
          createAttribute("com.google.cloud.tools.eclipse.appengine.libraries.groupid", "groupId"),
          createAttribute("com.google.cloud.tools.eclipse.appengine.libraries.artifactId", "artifactId")
    };
    MavenCoordinates mavenCoordinates = MavenCoordinatesHelper.createMavenCoordinates(attributes);
    assertThat(mavenCoordinates.getGroupId(), is("groupId"));
    assertThat(mavenCoordinates.getArtifactId(), is("artifactId"));
  }

  @Test
  public void testCreateMavenCoordinates_withRepository() {
    IClasspathAttribute[] attributes =
        addToDefaultAttributes("com.google.cloud.tools.eclipse.appengine.libraries.repository", "repo");
    MavenCoordinates mavenCoordinates = MavenCoordinatesHelper.createMavenCoordinates(attributes);
    assertThat(mavenCoordinates.getRepository(), is("repo"));
  }

  @Test
  public void testCreateMavenCoordinates_withType() {
    IClasspathAttribute[] attributes =
        addToDefaultAttributes("com.google.cloud.tools.eclipse.appengine.libraries.type", "war");
    MavenCoordinates mavenCoordinates = MavenCoordinatesHelper.createMavenCoordinates(attributes);
    assertThat(mavenCoordinates.getType(), is("war"));
  }

  @Test
  public void testCreateMavenCoordinates_withVersion() {
    IClasspathAttribute[] attributes =
        addToDefaultAttributes("com.google.cloud.tools.eclipse.appengine.libraries.version", "1.0.0");
    MavenCoordinates mavenCoordinates = MavenCoordinatesHelper.createMavenCoordinates(attributes);
    assertThat(mavenCoordinates.getVersion(), is("1.0.0"));
  }

  @Test
  public void testCreateMavenCoordinates_withClassifier() {
    IClasspathAttribute[] attributes =
        addToDefaultAttributes("com.google.cloud.tools.eclipse.appengine.libraries.classifier", "sources");
    MavenCoordinates mavenCoordinates = MavenCoordinatesHelper.createMavenCoordinates(attributes);
    assertThat(mavenCoordinates.getClassifier(), is("sources"));
  }

  protected IClasspathAttribute[] addToDefaultAttributes(String name, String value) {
    IClasspathAttribute[] attributes = 
        new IClasspathAttribute[] { 
          createAttribute("com.google.cloud.tools.eclipse.appengine.libraries.groupid", "groupId"),
          createAttribute("com.google.cloud.tools.eclipse.appengine.libraries.artifactId", "artifactId"),
          createAttribute(name, value)
    };
    return attributes;
  }

  private IClasspathAttribute createAttribute(final String name, final String value) {
    return new IClasspathAttribute() {
      @Override
      public String getName() {
        return name;
      }
      @Override
      public String getValue() {
        return value;
      }
    };
  }

  private void assertAttribute(List<IClasspathAttribute> classpathAttributes,
                               String attributeName,
                               String attributeValue) {
    for (IClasspathAttribute attribute : classpathAttributes) {
      if (attribute.getName().equals(attributeName)) {
        assertThat(attribute.getValue(), is(attributeValue));
        return;
      }
    }
    fail(MessageFormat.format("Attribute {0} was not found", attributeName));
  }

  
}
