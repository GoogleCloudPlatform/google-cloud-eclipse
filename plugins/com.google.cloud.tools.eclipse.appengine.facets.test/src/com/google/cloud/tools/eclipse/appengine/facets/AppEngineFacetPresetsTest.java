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

package com.google.cloud.tools.eclipse.appengine.facets;

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
 * Test the App Engine facet presets.
 */
public class AppEngineFacetPresetsTest {
  @Test
  public void appEngineStandardJava7presetExists() {
    IPreset preset = ProjectFacetsManager
        .getPreset("com.google.cloud.tools.eclipse.appengine.standard.jre7.preset");
    assertNotNull(preset);
    assertEquals(
        "App Engine standard environment with Java 7, Servlet 2.5 (deprecated)", preset.getLabel());
    assertThat(preset.getProjectFacets(), hasItem(JavaFacet.VERSION_1_7));
    assertThat(preset.getProjectFacets(), hasItem(WebFacetUtils.WEB_25));
    assertThat(preset.getProjectFacets(), hasItem(AppEngineStandardFacet.JRE7));
  }

  @Test
  public void appEngineFlexibleWarPresetExists() {
    IPreset preset = ProjectFacetsManager
        .getPreset("com.google.cloud.tools.eclipse.appengine.flexible.war.preset");
    assertNotNull(preset);
    assertEquals("App Engine flexible environment with Java 8, Servlet 3.1", preset.getLabel());
    assertThat(preset.getProjectFacets(), hasItem(JavaFacet.VERSION_1_8));
    assertThat(preset.getProjectFacets(), hasItem(WebFacetUtils.WEB_31));
    assertThat(preset.getProjectFacets(), hasItem(AppEngineFlexWarFacet.FACET_VERSION));
  }

  @Test
  public void appEngineFlexibleJarPresetExists() {
    IPreset preset = ProjectFacetsManager
        .getPreset("com.google.cloud.tools.eclipse.appengine.flexible.jar.preset");
    assertNotNull(preset);
    assertEquals("App Engine flexible environment with Java 8, runnable JAR", preset.getLabel());
    assertThat(preset.getProjectFacets(), hasItem(JavaFacet.VERSION_1_8));
    assertThat(preset.getProjectFacets(), hasItem(AppEngineFlexJarFacet.FACET_VERSION));
  }
}
