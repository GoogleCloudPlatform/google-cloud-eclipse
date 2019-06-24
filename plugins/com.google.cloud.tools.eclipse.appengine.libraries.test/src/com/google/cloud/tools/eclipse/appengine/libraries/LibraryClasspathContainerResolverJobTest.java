/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.util.jobs.MutexRule;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.Test;
import org.mockito.Mockito;

public class LibraryClasspathContainerResolverJobTest {

  @Test
  public void testBelongsTo() {
    ISchedulingRule rule = new MutexRule("LibraryClasspathContainerResolverJobTest");
    ILibraryClasspathContainerResolverService service =
        Mockito.mock(ILibraryClasspathContainerResolverService.class);
    IJavaProject javaProject = Mockito.mock(IJavaProject.class);
    LibraryClasspathContainerResolverJob fixture =
        new LibraryClasspathContainerResolverJob(rule, service, javaProject);

    assertTrue(fixture.belongsTo(ResourcesPlugin.FAMILY_MANUAL_BUILD));
  }
}
