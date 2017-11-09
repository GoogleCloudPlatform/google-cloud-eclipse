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
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A single option in a {@link PipelineOptionsType}.
 */
public class PipelineOptionsProperty {
  private final String name;
  private boolean defaultProvided;
  private final boolean required;
  private final Set<String> groups;

  private final String description;

  public static PipelineOptionsProperty fromMethod(IMethod method, MajorVersion majorVersion) {
    String methodName = method.getElementName();
    String propertyName = getPropertyName(methodName);
    if (propertyName == null) {
      return null;
    }
    IAnnotation required =
        method.getAnnotation(PipelineOptionsNamespaces.validationRequired(majorVersion));
    Requirement requirement = Requirement.fromAnnotation(required, majorVersion);
    boolean defaultProvided = false;
    try {
      for (IAnnotation annotation : method.getAnnotations()) {
        if (annotation
            .getElementName()
            .contains(PipelineOptionsNamespaces.defaultProvider(majorVersion))) {
          defaultProvided = true;
          break;
        }
      }
    } catch (JavaModelException e) {
      DataflowCorePlugin.logError(
          e, "Error while trying to find if default provided on method %s; assuming false",
          method.getElementName());
    }

    IAnnotation descriptionAnnotation =
        method.getAnnotation(PipelineOptionsNamespaces.descriptionAnnotation(majorVersion));
    String descriptionValue = null;
    if (descriptionAnnotation.exists()) {
      try {
        IMemberValuePair memberValuePair = descriptionAnnotation.getMemberValuePairs()[0];
        if (memberValuePair.getValueKind() == IMemberValuePair.K_STRING) {
          descriptionValue = (String) memberValuePair.getValue();
        } else {
          DataflowCorePlugin.logWarning(
              "Retrieved Description annotation but value is not of type String: %s",
              descriptionAnnotation);
        }
      } catch (JavaModelException e) {
        DataflowCorePlugin.logWarning(
            "Exception while retreiving description for existing Description annotation %s",
            descriptionAnnotation);
      }
    }
    return new PipelineOptionsProperty(propertyName, defaultProvided, requirement.isRequired(),
        requirement.getGroups(), descriptionValue);
  }

  private static String getPropertyName(String methodName) {
    if (methodName.startsWith("get")) {
      String capitalizedPropertyName = methodName.substring(3);
      String propertyName =
          capitalizedPropertyName.substring(0, 1).toLowerCase()
              + capitalizedPropertyName.substring(1);
      return propertyName;
    } else if (methodName.startsWith("is")) {
      String capitalizedPropertyName = methodName.substring(2);
      String propertyName =
          capitalizedPropertyName.substring(0, 1).toLowerCase()
              + capitalizedPropertyName.substring(1);
      return propertyName;
    }
    return null;
  }

  public PipelineOptionsProperty(String name, boolean defaultProvided, boolean required,
      Set<String> groups, String description) {
    this.name = name;
    this.defaultProvided = defaultProvided;
    this.required = required;
    this.groups = groups;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public boolean isDefaultProvided() {
    return defaultProvided;
  }

  public boolean isRequired() {
    return required;
  }

  public boolean isUserValueRequired() {
    return required && !defaultProvided;
  }

  public Set<String> getGroups() {
    return groups;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (obj instanceof PipelineOptionsProperty) {
      PipelineOptionsProperty that = (PipelineOptionsProperty) obj;
      return Objects.equals(this.name, that.name);
    }
    return false;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass())
        .add("name", name)
        .add("required", required)
        .add("groups", groups)
        .toString();
  }

  /**
   * The requirement restrictions of a {@link PipelineOptionsProperty}.
   */
  private static class Requirement {
    private final boolean required;
    private final Set<String> groups;

    private static Requirement fromAnnotation(
        IAnnotation requiredAnnotation, MajorVersion majorVersion) {
      if (!requiredAnnotation.exists()) {
        return new Requirement(false, Collections.<String>emptySet());
      }
      IMemberValuePair[] memberValuePairs;
      try {
        memberValuePairs = requiredAnnotation.getMemberValuePairs();
      } catch (JavaModelException e) {
        DataflowCorePlugin.logError(e, "Error while retrieving Member Value Pairs for"
            + " Validation.Required annotation %s in Java Element %s", requiredAnnotation,
            requiredAnnotation.getParent());
        return new Requirement(true, Collections.<String>emptySet());
      }
      for (IMemberValuePair memberValuePair : memberValuePairs) {
        String memberName = memberValuePair.getMemberName();
        Object memberValueObj = memberValuePair.getValue();
        if (memberName.equals(PipelineOptionsNamespaces.validationRequiredGroupField(majorVersion))
            && memberValueObj instanceof Object[]
            && memberValuePair.getValueKind() == IMemberValuePair.K_STRING) {
          Set<String> groups = new HashSet<>();
          for (Object group : (Object[]) memberValueObj) {
            groups.add(group.toString());
          }
          return new Requirement(true, groups);
        }
      }
      return new Requirement(true, Collections.<String>emptySet());
    }

    private Requirement(boolean required, Set<String> groups) {
      this.required = required;
      this.groups = ImmutableSet.copyOf(groups);
    }

    public boolean isRequired() {
      return required;
    }

    public Set<String> getGroups() {
      return groups;
    }
  }
}
