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

package com.google.cloud.tools.eclipse.util;

import com.google.common.collect.Lists;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class ClasspathUtil {

  public static void addClasspathEntry(IProject project, IClasspathEntry toAdd,
      IProgressMonitor monitor) throws JavaModelException {
    addClasspathEntries(project, Lists.newArrayList(toAdd), monitor);
  }

  public static void addClasspathEntries(IProject project, List<IClasspathEntry> toAdd,
      IProgressMonitor monitor) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    List<IClasspathEntry> entries = Lists.newArrayList(javaProject.getRawClasspath());
    entries.addAll(toAdd);
    javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[0]), monitor);
  }

}
