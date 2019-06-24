/*
 * Copyright 2017 Google LLC
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

package com.google.cloud.tools.eclipse.integration.appengine;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.junit.BeforeClass;

public class ProjectDependencyPublishTest extends ChildModuleWarPublishTest {

  @BeforeClass
  public static void setUp() throws IOException, CoreException {
    loadTestProjectZip("test-projects/project-dep-publish-example.zip", "dep_publish");
  }

  @Override
  protected List<String> getExpectedChildModuleNames() {
    return Arrays.asList("simple-dep-0.0.1-SNAPSHOT.jar", "simple-dep-b-0.0.1-SNAPSHOT.jar");
  }

}
