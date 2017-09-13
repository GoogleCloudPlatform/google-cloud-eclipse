/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.standard.java8;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IPreset;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Test;

/**
 * Test the App Engine standard environment for Java 8 preset.
 */
public class AppEngineStandardJava8PresetTest {
  @Test
  public void presetExists() {
    IPreset preset = ProjectFacetsManager
        .getPreset("com.google.cloud.tools.eclipse.appengine.standard.jre8.preset");
    assertNotNull(preset);
    assertEquals("App Engine standard environment with Java 8, Servlet 3.1",
        preset.getLabel());
    assertThat(preset.getProjectFacets(), hasItem(JavaFacet.VERSION_1_8));
    assertThat(preset.getProjectFacets(), hasItem(WebFacetUtils.WEB_31));
    assertThat(preset.getProjectFacets(),
        hasItem(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8));
  }
}
