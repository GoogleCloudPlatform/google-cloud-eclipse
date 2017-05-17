/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.launcher;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaMainTab;

import java.util.ArrayList;
import java.util.List;

/**
 * The Launch Configuration Tab Group for Dataflow Pipelines.
 *
 * <p>Provides Run Locally and Run Remotely configuration for launching a Dataflow Pipeline.
 */
public class DataflowPipelineLaunchConfigurationTabGroup extends
    AbstractLaunchConfigurationTabGroup {

  @Override
  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    // Use the same tab layout as the Java tabs, except with a custom Arguments tab
    List<ILaunchConfigurationTab> tabs = new ArrayList<>();
    JavaMainTab javaMainTab = new JavaMainTab();
    tabs.add(javaMainTab);

    PipelineArgumentsTab pipelineArgumentsTab = new PipelineArgumentsTab();
    tabs.add(pipelineArgumentsTab);

    JavaArgumentsTab javaArgumentsTab = new JavaArgumentsTab();
    tabs.add(javaArgumentsTab);

    JavaJRETab jreTab = new JavaJRETab();
    tabs.add(jreTab);

    JavaClasspathTab classpathTab = new JavaClasspathTab();
    tabs.add(classpathTab);

    SourceLookupTab sourceLookupTab = new SourceLookupTab();
    tabs.add(sourceLookupTab);

    EnvironmentTab environmentTab = new EnvironmentTab();
    tabs.add(environmentTab);

    CommonTab commonTab = new CommonTab();
    tabs.add(commonTab);

    setTabs(tabs.toArray(new ILaunchConfigurationTab[0]));
  }

}
