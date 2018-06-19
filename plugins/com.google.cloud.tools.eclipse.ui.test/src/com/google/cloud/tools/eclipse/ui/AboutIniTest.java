/*
 * Copyright 2018 Google LLC
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

package com.google.cloud.tools.eclipse.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.EclipseProperties;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;

/** Verify that the requirements for a branding feature plugin are met. */
public class AboutIniTest {
  @Rule public final EclipseProperties aboutIni = new EclipseProperties("about.ini");
  @Rule public final EclipseProperties aboutProperties = new EclipseProperties("about.properties");

  @Test
  public void testFeatureImage() {
    assertNotNull(aboutIni.get("featureImage"));
    assertEquals("icons/about/gcp.png", aboutIni.get("featureImage"));
    assertTrue(new File("../com.google.cloud.tools.eclipse.ui/icons/about/gcp.png").exists());
  }

  public void testAboutText() {
    assertNotNull(aboutIni.get("aboutText"));
    assertEquals("%blurb", aboutIni.get("aboutText"));

    assertNotNull(aboutProperties.get("blurb"));
    assertEquals(
        "Google Cloud Tools for Eclipse\n"
            + "Version: {featureVersion}\n"
            + "\n"
            + "Copyright 2016-2018 Google LLC\n"
            + "\n"
            + "Cloud Tools for Eclipse is a Google-sponsored plugin that adds \n"
            + "support for App Engine, Cloud Dataflow, and other parts of \n"
            + "the Google Cloud Platform to the Eclipse IDE.\n"
            + "Visit: https://cloud.google.com/eclipse",
        aboutProperties.get("blurb"));
  }
}
