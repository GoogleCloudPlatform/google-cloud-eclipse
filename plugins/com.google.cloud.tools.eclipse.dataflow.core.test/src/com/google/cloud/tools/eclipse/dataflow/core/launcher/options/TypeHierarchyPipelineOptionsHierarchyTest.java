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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link JavaProjectPipelineOptionsHierarchy}.
 */
@RunWith(Parameterized.class)
public class TypeHierarchyPipelineOptionsHierarchyTest {
  @Parameters
  public static Iterable<? extends Object> versions() {
    return ImmutableList.of(MajorVersion.ONE, MajorVersion.TWO);
  }

  private MajorVersion version;
  public TypeHierarchyPipelineOptionsHierarchyTest(MajorVersion version) {
    this.version = version;
  }

  private JavaProjectPipelineOptionsHierarchy pipelineOptionsHierarchy;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  private IJavaProject project;

  @Mock
  private ITypeHierarchy typeHierarchy;

  private IType rootType;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    rootType = mockType(PipelineOptionsNamespaces.rootType(version));
    when(rootType.getMethods()).thenReturn(new IMethod[0]);
    when(rootType.exists()).thenReturn(true);

    when(project.findType(PipelineOptionsNamespaces.rootType(version)))
        .thenReturn(rootType);

    when(rootType.newTypeHierarchy(Mockito.<IProgressMonitor>any())).thenReturn(typeHierarchy);
    when(typeHierarchy.getSuperInterfaces(rootType)).thenReturn(new IType[0]);

    pipelineOptionsHierarchy =
        new JavaProjectPipelineOptionsHierarchy(project, version, new NullProgressMonitor());
  }

  @Test
  public void testGetPipelineOptionsTypeReturnsPipelineOptions() throws Exception {
    String intermediateTypeName = "com.google.IntermediateOptionsType";
    IType intermediateType = mockType(intermediateTypeName);
    when(intermediateType.getMethods()).thenReturn(new IMethod[0]);

    String requestedTypeName = "foo.bar.RequestedType";
    IType requestedType = mockType(requestedTypeName);
    when(requestedType.getMethods()).thenReturn(new IMethod[0]);

    when(project.findType(requestedTypeName)).thenReturn(requestedType);

    when(rootType.newTypeHierarchy(Mockito.<IProgressMonitor>any())).thenReturn(typeHierarchy);
    when(typeHierarchy.contains(requestedType)).thenReturn(true);

    when(typeHierarchy.getSuperInterfaces(requestedType))
        .thenReturn(new IType[] {intermediateType});
    when(typeHierarchy.getSuperInterfaces(intermediateType)).thenReturn(new IType[] {rootType});
    when(typeHierarchy.getSuperInterfaces(rootType)).thenReturn(new IType[0]);

    PipelineOptionsType type = pipelineOptionsHierarchy.getPipelineOptionsType(requestedTypeName);

    assertEquals(requestedTypeName, type.getName());
    assertEquals(3, pipelineOptionsHierarchy.getOptionsHierarchy(requestedTypeName).size());
  }

  @Test
  public void testGetAllPipelineOptionsTypesReturnsMapOfClassesInPipelineOptionsTypeHierarchy()
      throws Exception {
    IType rootType = mockType(PipelineOptionsNamespaces.rootType(version));
    when(rootType.exists()).thenReturn(true);
    when(rootType.getMethods()).thenReturn(new IMethod[] {});

    when(project.findType(PipelineOptionsNamespaces.rootType(version)))
        .thenReturn(rootType);

    String childTypeName = "com.google.cloud.dataflow.ExtendedOptions";
    IType childType = mockType(childTypeName);
    when(childType.getMethods()).thenReturn(new IMethod[] {});

    IType[] optionsTypes = new IType[] {rootType, childType};

    when(typeHierarchy.getAllInterfaces()).thenReturn(optionsTypes);
    when(typeHierarchy.getSuperInterfaces(rootType)).thenReturn(new IType[0]);
    when(typeHierarchy.getSuperInterfaces(childType)).thenReturn(new IType[] {rootType});

    when(rootType.newTypeHierarchy(Mockito.<IProgressMonitor>any())).thenReturn(typeHierarchy);

    Map<String, PipelineOptionsType> pipelineOptions =
        pipelineOptionsHierarchy.getAllPipelineOptionsTypes();

    assertEquals(2, pipelineOptions.size());

    assertTrue(pipelineOptions.containsKey(
        PipelineOptionsNamespaces.rootType(version)));
    assertTrue(pipelineOptions.containsKey(childTypeName));

    PipelineOptionsType rootPipelineOptionsType =
        pipelineOptions.get(PipelineOptionsNamespaces.rootType(version));
    assertEquals(
        PipelineOptionsNamespaces.rootType(version),
        rootPipelineOptionsType.getName());

    PipelineOptionsType childPipelineOptionsType = pipelineOptions.get(childTypeName);
    assertEquals(childTypeName, childPipelineOptionsType.getName());
  }

  @Test
  public void testGetPipelineOptionsTypeForTypeNotInProjectReturnsAbsent() throws Exception {
    IType rootType = mockType(PipelineOptionsNamespaces.rootType(version));
    when(rootType.getMethods()).thenReturn(new IMethod[0]);
    when(rootType.exists()).thenReturn(true);

    when(project.findType(PipelineOptionsNamespaces.rootType(version)))
        .thenReturn(rootType);

    String requestedTypeName = "foo.bar.RequestedType";
    when(project.findType(requestedTypeName)).thenReturn(null);

    ITypeHierarchy hierarchy = mock(ITypeHierarchy.class);
    when(rootType.newTypeHierarchy(Mockito.<IProgressMonitor>any())).thenReturn(hierarchy);

    PipelineOptionsType type =
        pipelineOptionsHierarchy.getPipelineOptionsType(requestedTypeName);

    assertNull(type);
  }

  /**
   * Test that sortedHierarchy returns a topologically sorted list of {@link PipelineOptionsType}
   * instances.
   *
   * <p>The order in which Pipeline Options Types are returned is such that for any
   * {@link PipelineOptionsType}, that type precedes any {@link PipelineOptionsType} that contains
   * it.
   */
  @Test
  public void testGetOptionsHierarchyReturnsOptionsInHierarchicalOrder() throws Exception {
    PipelineOptionsType root = new PipelineOptionsType(
        PipelineOptionsNamespaces.rootType(version),
        Collections.<PipelineOptionsType>emptySet(),
        Collections.<PipelineOptionsProperty>emptySet());
    PipelineOptionsType secondHighest = new PipelineOptionsType(
        "NearlyFirst", ImmutableSet.of(root), Collections.<PipelineOptionsProperty>emptySet());
    PipelineOptionsType medium = new PipelineOptionsType("Medium",
        ImmutableSet.of(secondHighest, root), Collections.<PipelineOptionsProperty>emptySet());
    PipelineOptionsType lowest =
        new PipelineOptionsType("Lowest", ImmutableSet.of(root, medium, secondHighest),
            Collections.<PipelineOptionsProperty>emptySet());

    IType low = mockType("Lowest");
    IType med = mockType("Medium");
    IType sec = mockType("NearlyFirst");
    when(typeHierarchy.getSuperInterfaces(low)).thenReturn(new IType[] {med, sec, rootType});
    when(typeHierarchy.getSuperInterfaces(med)).thenReturn(new IType[] {sec, rootType});
    when(typeHierarchy.getSuperInterfaces(sec)).thenReturn(new IType[] {rootType});

    when(project.findType("Lowest")).thenReturn(low);
    when(low.newTypeHierarchy(Mockito.<IProgressMonitor>any())).thenReturn(typeHierarchy);
    when(typeHierarchy.contains(low)).thenReturn(true);

    when(low.getMethods()).thenReturn(new IMethod[0]);
    when(med.getMethods()).thenReturn(new IMethod[0]);
    when(sec.getMethods()).thenReturn(new IMethod[0]);

    Set<PipelineOptionsType> hierarchy =
        pipelineOptionsHierarchy.getOptionsHierarchy("Lowest").keySet();

    Set<PipelineOptionsType> expectedHierarchy =
        ImmutableSet.of(lowest, medium, secondHighest, root);
    assertEquals(expectedHierarchy, hierarchy);

    // Create a list so the comparison is order sensitive.
    assertEquals(ImmutableList.copyOf(expectedHierarchy), ImmutableList.copyOf(hierarchy));
  }

  public IType mockType(String typeName) {
    IType rootType = mock(IType.class);
    when(rootType.getFullyQualifiedName()).thenReturn(typeName);
    return rootType;
  }
}
