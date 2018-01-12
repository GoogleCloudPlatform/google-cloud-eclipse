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

package com.google.cloud.tools.eclipse.test.util;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Generic tests that should be true of all plugins.
 */
public abstract class BaseBuildPropertiesTest {

  @Rule
  public final EclipseProperties buildProperties = new EclipseProperties("build.properties");
  
  @Test
  public final void testBuildProperties() throws IOException {
    String[] binIncludes = buildProperties.get("bin.includes").split(",\\s*");
    Set<String> includes = Sets.newHashSet(binIncludes);

    Assert.assertTrue(includes.contains("."));
    Assert.assertTrue(includes.contains("META-INF/"));
    
    testIncludedIfPresent(includes, "helpContexts.xml");
    testIncludedIfPresent(includes, "icons/");
    testIncludedIfPresent(includes, "lib/");
    testIncludedIfPresent(includes, "README.md");
    testIncludedIfPresent(includes, "epl-v10.html");
    testIncludedIfPresent(includes, "OSGI-INF/");
    testIncludedIfPresent(includes, "fragment.xml");
    testIncludedIfPresent(includes, "fragment.properties");
    testIncludedIfPresent(includes, "lifecycle-mapping-metadata.xml"); // for m2e extensions
  }

  private static void testIncludedIfPresent(Set<String> includes, String name) 
      throws IOException {
    String path = EclipseProperties.getHostBundlePath() + "/" + name;
    if (Files.exists(Paths.get(path))) {
      Assert.assertTrue(includes.contains(name));
    }
  }
}
