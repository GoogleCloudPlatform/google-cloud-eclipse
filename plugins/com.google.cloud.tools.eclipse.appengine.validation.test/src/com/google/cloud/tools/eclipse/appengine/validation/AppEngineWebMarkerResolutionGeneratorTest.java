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

package com.google.cloud.tools.eclipse.appengine.validation;

import static org.junit.Assert.assertEquals;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.junit.Test;
import org.mockito.Mockito;

public class AppEngineWebMarkerResolutionGeneratorTest {
  
  @Test
  public void testGetResolutions_versionElement() throws CoreException {
    AppEngineWebMarkerResolutionGenerator resolution = new AppEngineWebMarkerResolutionGenerator();
    IMarker marker = Mockito.mock(IMarker.class);
    Mockito.when(marker.getType())
        .thenReturn("com.google.cloud.tools.eclipse.appengine.validation.versionMarker");
    IMarkerResolution[] resolutions = resolution.getResolutions(marker);
    assertEquals(1, resolutions.length);
    assertEquals(VersionQuickFix.class, resolutions[0].getClass());
  }
  
  @Test
  public void testGetResolutions_applicationElement() throws CoreException {
    AppEngineWebMarkerResolutionGenerator resolution = new AppEngineWebMarkerResolutionGenerator();
    IMarker marker = Mockito.mock(IMarker.class);
    Mockito.when(marker.getType())
        .thenReturn("com.google.cloud.tools.eclipse.appengine.validation.applicationMarker");
    IMarkerResolution[] resolutions = resolution.getResolutions(marker);
    assertEquals(1, resolutions.length);
    assertEquals(ApplicationQuickFix.class, resolutions[0].getClass());
  }


}
