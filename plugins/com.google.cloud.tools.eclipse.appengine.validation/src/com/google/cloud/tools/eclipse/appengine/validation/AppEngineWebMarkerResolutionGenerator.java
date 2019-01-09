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

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

/**
 * Returns possible resolutions for given resource marker. Quick Fix resolutions
 * are available in the Problems view.
 */
class AppEngineWebMarkerResolutionGenerator implements IMarkerResolutionGenerator {

  private static final Logger logger =
      Logger.getLogger(AppEngineWebMarkerResolutionGenerator.class.getName());
   
  @Override
  public IMarkerResolution[] getResolutions(IMarker marker) {
    
    ArrayList<IMarkerResolution> resolutions = new ArrayList<>();
    try {
      if ("com.google.cloud.tools.eclipse.appengine.validation.applicationMarker"
          .equals(marker.getType())) {
        resolutions.add(new ApplicationQuickFix());
      } else if ("com.google.cloud.tools.eclipse.appengine.validation.versionMarker"
          .equals(marker.getType())) {
        resolutions.add(new VersionQuickFix());
      } else if ("com.google.cloud.tools.eclipse.appengine.validation.runtimeMarker"
          .equals(marker.getType())) {
        resolutions.add(new UpgradeRuntimeQuickFix());
      }
    } catch (CoreException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
    return resolutions.toArray(new IMarkerResolution[0]);
  }
  
}
