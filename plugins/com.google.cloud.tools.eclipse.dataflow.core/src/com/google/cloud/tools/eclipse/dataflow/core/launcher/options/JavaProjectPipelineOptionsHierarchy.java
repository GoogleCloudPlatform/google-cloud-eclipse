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

import com.google.cloud.tools.eclipse.dataflow.core.DataflowCorePlugin;
import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

/**
 * A {@link PipelineOptionsHierarchy} that uses an {@code ITypeHierarchy} as the source of the
 * {@code PipelineOptionsHierarchy}.
 */
public class JavaProjectPipelineOptionsHierarchy implements PipelineOptionsHierarchy {

  private final IJavaProject project;
  private final ITypeHierarchy hierarchy;

  private final MajorVersion majorVersion;

  /**
   * A cache of constructed PipelineOptionsType for each type in the hierarchy that has been
   * retrieved.
   */
  private final Map<String, PipelineOptionsType> knownTypes;

  /**
   * Creates a new {@link JavaProjectPipelineOptionsHierarchy}. This can be a long-running method,
   * as it fetches the type hierarchy of the provided project.
   *
   * @throws JavaModelException
   */
  public JavaProjectPipelineOptionsHierarchy(
      IJavaProject project, MajorVersion version, IProgressMonitor monitor)
      throws JavaModelException {
    IType rootType = project.findType(PipelineOptionsNamespaces.rootType(version));
    if (rootType == null || !rootType.exists()) {
      throw new IllegalArgumentException(
          "Tried to create a TypeHierarchyPipelineOptionsHierarchy for a Java Project "
              + "where no PipelineOptions type exists");
    }

    // Flatten the class hierarchy, recording all the classes present
    ITypeHierarchy hierarchy = rootType.newTypeHierarchy(monitor);

    this.project = project;
    this.hierarchy = hierarchy;
    this.majorVersion = version;
    this.knownTypes = new HashMap<>();
  }

  @Override
  public PipelineOptionsType getPipelineOptionsType(String typeName) {
    try {
      IType namedType = project.findType(typeName);
      if (namedType == null) {
        DataflowCorePlugin.logWarning(
            "Pipeline Options Type %s not found in PipelineOptions type hierarchy in project %s; "
                + "Hierarchy %s",
            typeName,
            project.getElementName(),
            this.hierarchy);
        return null;
      }
      if (hierarchy.contains(namedType)) {
        return getOrCreatePipelineOptionsType(namedType);
      }
    } catch (JavaModelException e) {
      DataflowCorePlugin.logError(e, "JavaModelException while retrieving type %s", typeName);
    }
    return null;
  }

  @Override
  public Map<String, PipelineOptionsType> getAllPipelineOptionsTypes() {
    IType[] types = hierarchy.getAllInterfaces();

    for (IType type : types) {
      try {
        getOrCreatePipelineOptionsType(type);
      } catch (JavaModelException e) {
        DataflowCorePlugin.logError(
            e, "Error while attempting to retrieve PipelineOptionsType %s. Skipping over.",
            type.getFullyQualifiedName());
      }
    }

    // Return a view of all the known types.
    return Collections.unmodifiableMap(knownTypes);
  }

  @Override
  public NavigableMap<PipelineOptionsType, Set<PipelineOptionsProperty>> getOptionsHierarchy(
      String... typeNames) {
    NavigableMap<PipelineOptionsType, Set<PipelineOptionsProperty>> result =
        new TreeMap<>(new PipelineOptionsTypeWeightOrdering());
    Queue<PipelineOptionsType> optionsTypesToAdd = new ArrayDeque<>();
    for (String typeName : typeNames) {
      if (!Strings.isNullOrEmpty(typeName)) {
        PipelineOptionsType pipelineOptionsType = getPipelineOptionsType(typeName);
        if (pipelineOptionsType != null) {
          optionsTypesToAdd.add(pipelineOptionsType);
        }
      }
    }
    while (!optionsTypesToAdd.isEmpty()) {
      PipelineOptionsType type = optionsTypesToAdd.poll();
      if (!result.containsKey(type)) {
        result.put(type, type.getDeclaredProperties());
        optionsTypesToAdd.addAll(type.getDirectSuperInterfaces());
      }
    }
    return result.descendingMap();
  }

  @Override
  public Map<PipelineOptionsType, Set<PipelineOptionsProperty>> getRequiredOptionsByType(
      String... baseTypeNames) {
    Map<PipelineOptionsType, Set<PipelineOptionsProperty>> requiredOptions = new LinkedHashMap<>();
    for (Map.Entry<PipelineOptionsType, Set<PipelineOptionsProperty>> optionsEntry :
        getOptionsHierarchy(baseTypeNames).entrySet()) {
      Set<PipelineOptionsProperty> properties = new HashSet<>();
      for (PipelineOptionsProperty property : optionsEntry.getValue()) {
        if (property.isRequired()) {
          properties.add(property);
        }
      }
      if (!properties.isEmpty()) {
        requiredOptions.put(optionsEntry.getKey(), properties);
      }
    }
    return requiredOptions;
  }

  @Override
  public Set<String> getPropertyNames(String... baseTypeNames) {
    Set<String> result = new LinkedHashSet<>();
    for (Set<PipelineOptionsProperty> optionsProperties :
        getOptionsHierarchy(baseTypeNames).values()) {
      for (PipelineOptionsProperty property : optionsProperties) {
        result.add(property.getName());
      }
    }
    return result;
  }

  /**
   * Retrieve the {@link PipelineOptionsType} for the provided {@code optionsType} using the
   * {@code
   * typeHierarchy}. If the {@code PipelineOptionsType} has already been created, reuse the same
   * instance (by fully qualified name).
   *
   * <p>If the {@code PipelineOptionsType} does not exist, create all of its supertypes that extend
   * {@code PipelineOptions} recursively, and create the requested type, place it in the {@code
   * pipelinOptionsTypes} map, and return it. If the {@code PipelineOptionsType} has already been
   * created, return it.
   */
  private PipelineOptionsType getOrCreatePipelineOptionsType(IType optionsType)
      throws JavaModelException {
    if (knownTypes.containsKey(optionsType.getFullyQualifiedName())) {
      // Already found this type on a different interface.
      return knownTypes.get(optionsType.getFullyQualifiedName());
    }

    // Construct the Superinterfaces of this type recursively.
    ImmutableSet.Builder<PipelineOptionsType> parentTypes = ImmutableSet.builder();
    for (IType superInterface : hierarchy.getSuperInterfaces(optionsType)) {
      PipelineOptionsType superInterfaceType = getOrCreatePipelineOptionsType(superInterface);
      parentTypes.add(superInterfaceType);
    }

    PipelineOptionsType myType = new PipelineOptionsType(
        optionsType.getFullyQualifiedName(), parentTypes.build(), getProperties(optionsType));
    knownTypes.put(optionsType.getFullyQualifiedName(), myType);
    return myType;
  }

  private Set<PipelineOptionsProperty> getProperties(IType optionsType) {
    try {
      ImmutableSet.Builder<PipelineOptionsProperty> propertiesBuilder = ImmutableSet.builder();
      for (IMethod method : optionsType.getMethods()) {
        PipelineOptionsProperty property = PipelineOptionsProperty.fromMethod(method, majorVersion);
        if (property != null) {
          propertiesBuilder.add(property);
        }
      }
      return propertiesBuilder.build();
    } catch (JavaModelException e) {
      DataflowCorePlugin.logError(
          e, "Error while retrieving Methods for Options type %s", optionsType);
      return Collections.emptySet();
    }
  }

  private static class PipelineOptionsTypeWeightOrdering extends Ordering<PipelineOptionsType> {
    @Override
    public int compare(PipelineOptionsType o1, PipelineOptionsType o2) {
      return ComparisonChain.start()
          .compare(o1.getWeight(), o2.getWeight())
          .compare(o1.getName(), o2.getName())
          .result();
    }
  }
}
