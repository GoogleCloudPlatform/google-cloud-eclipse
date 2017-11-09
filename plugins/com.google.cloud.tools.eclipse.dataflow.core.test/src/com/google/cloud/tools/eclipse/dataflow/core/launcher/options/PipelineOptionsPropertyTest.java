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

package com.google.cloud.tools.eclipse.dataflow.core.launcher.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import java.util.Collections;

/** Tests for {@link PipelineOptionsProperty}. */
@RunWith(Parameterized.class)
public class PipelineOptionsPropertyTest {
  @Parameters
  public static Iterable<? extends Object> versions() {
    return ImmutableList.of(MajorVersion.ONE, MajorVersion.TWO);
  }

  private MajorVersion version;

  public PipelineOptionsPropertyTest(MajorVersion version) {
    this.version = version;
  }

  @Test
  public void testEqualsWithEqualNamesAreEqual() {
    String sharedName = "name";
    PipelineOptionsProperty firstProperty =
        new PipelineOptionsProperty(sharedName, true, false, Collections.<String>emptySet(), null);
    PipelineOptionsProperty secondProperty =
        new PipelineOptionsProperty(
            sharedName, true, true, ImmutableSet.<String>of("GroupOne"), null);

    assertEquals(firstProperty, secondProperty);
  }

  @Test
  public void testEqualsWithDifferentNamesAreNotEqual() {
    String name = "name";
    PipelineOptionsProperty firstProperty =
        new PipelineOptionsProperty(name, true, false, Collections.<String>emptySet(), null);

    String otherName = "name-foo-other-and-not-shared";
    PipelineOptionsProperty notEqualProperty =
        new PipelineOptionsProperty(otherName, true, false, Collections.<String>emptySet(), null);
    assertNotEquals(notEqualProperty, firstProperty);
  }

  @Test
  public void testFromMethodOnNonGetterMethodReturnsNull() {
    IMethod method = mock(IMethod.class);
    when(method.getElementName()).thenReturn("doFoo");

    PipelineOptionsProperty property = PipelineOptionsProperty.fromMethod(method, version);

    assertNull(property);
  }

  @Test
  public void testFromMethodWithBooleanReturnValueAndIsPropertyReturnsProperty()
      throws JavaModelException {
    IMethod method = mock(IMethod.class);
    when(method.getElementName()).thenReturn("isFoo");

    IAnnotation required = mock(IAnnotation.class);
    when(method.getAnnotation(PipelineOptionsNamespaces.validationRequired(version)))
        .thenReturn(required);
    when(required.exists()).thenReturn(false);
    when(required.getElementName())
        .thenReturn(PipelineOptionsNamespaces.validationRequired(version));
    when(method.getAnnotations()).thenReturn(new IAnnotation[] {required});

    IAnnotation description = mock(IAnnotation.class);
    when(description.exists()).thenReturn(false);
    when(method.getAnnotation(PipelineOptionsNamespaces.descriptionAnnotation(version)))
        .thenReturn(description);

    PipelineOptionsProperty property = PipelineOptionsProperty.fromMethod(method, version);

    assertEquals("foo", property.getName());
    assertFalse(property.isRequired());
    assertTrue(property.getGroups().isEmpty());
  }

  @Test
  public void testFromMethodWithNoValidationRequiredAnnotationIsNotRequired()
      throws JavaModelException {
    IMethod method = mock(IMethod.class);
    when(method.getElementName()).thenReturn("getFoo");

    IAnnotation required = mock(IAnnotation.class);
    when(method.getAnnotation(PipelineOptionsNamespaces.validationRequired(version)))
        .thenReturn(required);
    when(required.exists()).thenReturn(false);
    when(required.getElementName())
        .thenReturn(PipelineOptionsNamespaces.validationRequired(version));
    when(method.getAnnotations()).thenReturn(new IAnnotation[] {required});

    IAnnotation description = mock(IAnnotation.class);
    when(description.exists()).thenReturn(false);
    when(method.getAnnotation(PipelineOptionsNamespaces.descriptionAnnotation(version)))
        .thenReturn(description);

    PipelineOptionsProperty property = PipelineOptionsProperty.fromMethod(method, version);

    assertEquals("foo", property.getName());
    assertFalse(property.isRequired());
    assertTrue(property.getGroups().isEmpty());
  }

  @Test
  public void testFromMethodWithValidationRequiredWithNoGroupsIsRequiredWithNoGroups()
      throws JavaModelException {
    IMethod method = mock(IMethod.class);
    when(method.getElementName()).thenReturn("getFoo");

    IAnnotation required = mock(IAnnotation.class);
    when(method.getAnnotation(PipelineOptionsNamespaces.validationRequired(version)))
        .thenReturn(required);
    when(required.exists()).thenReturn(true);
    when(required.getElementName())
        .thenReturn(PipelineOptionsNamespaces.validationRequired(version));
    when(method.getAnnotations()).thenReturn(new IAnnotation[] {required});

    IMemberValuePair[] memberValuePairs = new IMemberValuePair[0];
    when(required.getMemberValuePairs()).thenReturn(memberValuePairs);

    IAnnotation description = mock(IAnnotation.class);
    when(description.exists()).thenReturn(false);
    when(method.getAnnotation(PipelineOptionsNamespaces.descriptionAnnotation(version)))
        .thenReturn(description);

    PipelineOptionsProperty property = PipelineOptionsProperty.fromMethod(method, version);

    assertEquals("foo", property.getName());
    assertTrue(property.isRequired());
    assertTrue(property.getGroups().isEmpty());
  }

  @Test
  public void testFromMethodWithValidationRequiredAndGroupsIsRequiredWithGroups()
      throws JavaModelException {
    IMethod method = mock(IMethod.class);
    when(method.getElementName()).thenReturn("getFoo");

    IAnnotation required = mock(IAnnotation.class);
    when(method.getAnnotation(PipelineOptionsNamespaces.validationRequired(version)))
        .thenReturn(required);
    when(required.exists()).thenReturn(true);
    when(required.getElementName())
        .thenReturn(PipelineOptionsNamespaces.validationRequired(version));
    when(method.getAnnotations()).thenReturn(new IAnnotation[] {required});

    String firstGroup = "bar";
    String secondGroup = "baz";
    String[] groups = new String[] {firstGroup, secondGroup};

    IMemberValuePair groupPair = mock(IMemberValuePair.class);
    when(groupPair.getMemberName())
        .thenReturn(PipelineOptionsNamespaces.validationRequiredGroupField(version));
    when(groupPair.getValue()).thenReturn(groups);
    when(groupPair.getValueKind()).thenReturn(IMemberValuePair.K_STRING);

    IMemberValuePair[] memberValuePairs = new IMemberValuePair[] {groupPair};
    when(required.getMemberValuePairs()).thenReturn(memberValuePairs);

    IAnnotation description = mock(IAnnotation.class);
    when(description.exists()).thenReturn(false);
    when(method.getAnnotation(PipelineOptionsNamespaces.descriptionAnnotation(version)))
        .thenReturn(description);

    PipelineOptionsProperty property = PipelineOptionsProperty.fromMethod(method, version);

    assertEquals("foo", property.getName());
    assertTrue(property.isRequired());
    assertTrue(property.getGroups().contains(firstGroup));
    assertTrue(property.getGroups().contains(secondGroup));
  }

  @Test
  public void testFromMethodWithDefaultHasDefaultProvided() throws JavaModelException {
    IMethod method = mock(IMethod.class);
    when(method.getElementName()).thenReturn("getFoo");

    IAnnotation required = mock(IAnnotation.class);
    when(method.getAnnotation(PipelineOptionsNamespaces.validationRequired(version)))
        .thenReturn(required);
    when(required.exists()).thenReturn(true);
    when(required.getElementName())
        .thenReturn(PipelineOptionsNamespaces.validationRequired(version));

    IMemberValuePair[] memberValuePairs = new IMemberValuePair[0];
    when(required.getMemberValuePairs()).thenReturn(memberValuePairs);

    IAnnotation defaultAnnotation = mock(IAnnotation.class);
    when(defaultAnnotation.getElementName())
        .thenReturn(PipelineOptionsNamespaces.defaultProvider(version) + ".String");
    when(method.getAnnotations()).thenReturn(new IAnnotation[] {required, defaultAnnotation});

    IAnnotation description = mock(IAnnotation.class);
    when(description.exists()).thenReturn(false);
    when(method.getAnnotation(PipelineOptionsNamespaces.descriptionAnnotation(version)))
        .thenReturn(description);

    PipelineOptionsProperty property = PipelineOptionsProperty.fromMethod(method, version);

    assertEquals("foo", property.getName());
    assertTrue(property.isRequired());
    assertTrue(property.isDefaultProvided());
    assertFalse(property.isUserValueRequired());
  }

  @Test
  public void testFromMethodWithDescriptionHasDescription() throws Exception {
    IMethod method = mock(IMethod.class);
    when(method.getElementName()).thenReturn("getFoo");

    IAnnotation required = mock(IAnnotation.class);
    when(method.getAnnotation(PipelineOptionsNamespaces.validationRequired(version)))
        .thenReturn(required);
    when(required.exists()).thenReturn(false);
    when(required.getElementName())
        .thenReturn(PipelineOptionsNamespaces.validationRequired(version));
    when(method.getAnnotations()).thenReturn(new IAnnotation[] {required});

    IAnnotation description = mock(IAnnotation.class);
    when(description.exists()).thenReturn(true);
    when(method.getAnnotation(PipelineOptionsNamespaces.descriptionAnnotation(version)))
        .thenReturn(description);
    IMemberValuePair memberValuePair = mock(IMemberValuePair.class);
    String descriptionValue = "My_description exists";
    when(memberValuePair.getValue()).thenReturn(descriptionValue);
    when(memberValuePair.getValueKind()).thenReturn(IMemberValuePair.K_STRING);
    IMemberValuePair[] memberValuePairs = new IMemberValuePair[] {memberValuePair};
    when(description.getMemberValuePairs()).thenReturn(memberValuePairs);

    PipelineOptionsProperty property = PipelineOptionsProperty.fromMethod(method, version);

    assertEquals(descriptionValue, property.getDescription());
  }

  @Test
  public void testFromMethodWithoutDescriptionHasAbsentOptional() throws Exception {
    IMethod method = mock(IMethod.class);
    when(method.getElementName()).thenReturn("getFoo");

    IAnnotation required = mock(IAnnotation.class);
    when(method.getAnnotation(PipelineOptionsNamespaces.validationRequired(version)))
        .thenReturn(required);
    when(required.exists()).thenReturn(false);
    when(required.getElementName())
        .thenReturn(PipelineOptionsNamespaces.validationRequired(version));
    when(method.getAnnotations()).thenReturn(new IAnnotation[] {required});

    IAnnotation description = mock(IAnnotation.class);
    when(description.exists()).thenReturn(false);
    when(method.getAnnotation(PipelineOptionsNamespaces.descriptionAnnotation(version)))
        .thenReturn(description);

    PipelineOptionsProperty property = PipelineOptionsProperty.fromMethod(method, version);

    assertNull(property.getDescription());
  }

  @Test
  public void testFromMethodWithNonStringDescriptionHasAbsentOptional() throws Exception {
    IMethod method = mock(IMethod.class);
    when(method.getElementName()).thenReturn("getFoo");

    IAnnotation required = mock(IAnnotation.class);
    when(method.getAnnotation(PipelineOptionsNamespaces.validationRequired(version)))
        .thenReturn(required);
    when(required.exists()).thenReturn(false);
    when(required.getElementName())
        .thenReturn(PipelineOptionsNamespaces.validationRequired(version));
    when(method.getAnnotations()).thenReturn(new IAnnotation[] {required});

    IAnnotation description = mock(IAnnotation.class);
    when(description.exists()).thenReturn(true);
    when(method.getAnnotation(PipelineOptionsNamespaces.descriptionAnnotation(version)))
        .thenReturn(description);
    IMemberValuePair memberValuePair = mock(IMemberValuePair.class);
    when(memberValuePair.getValue()).thenReturn(3);
    when(memberValuePair.getValueKind()).thenReturn(IMemberValuePair.K_INT);
    IMemberValuePair[] memberValuePairs = new IMemberValuePair[] {memberValuePair};
    when(description.getMemberValuePairs()).thenReturn(memberValuePairs);

    PipelineOptionsProperty property = PipelineOptionsProperty.fromMethod(method, version);

    assertNull(property.getDescription());
  }
}
