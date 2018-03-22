/*
 * Copyright 2015 Google LLC. All Rights Reserved.
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

import com.google.cloud.tools.eclipse.dataflow.ui.DataflowUiPlugin;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;

/**
 * A launch shortcut that launches a Dataflow Pipeline. The Launch configurations generated by this
 * shortcut are named after the project they are launching, and any other relevant automatically
 * detected resource, and an existing configuration based on the active selection or Editor Part is
 * automatically launched if possible.
 */
public class LaunchPipelineShortcut implements ILaunchShortcut2 {
  private static final String DATAFLOW_LAUNCH_CONFIGURATION_TYPE_ID =
      "com.google.cloud.dataflow.DataflowPipeline";

  private void launchExisting(ILaunchConfiguration configuration, String mode) {
    DebugUITools.launch(configuration, mode);
  }

  private void launchNew(LaunchableResource resource, String mode) {
    try {
      ILaunchManager launchManager = getLaunchManager();

      String launchConfigurationName =
          launchManager.generateLaunchConfigurationName(resource.getLaunchName());
      ILaunchConfigurationType configurationType =
          getDataflowLaunchConfigurationType(launchManager);
      ILaunchConfigurationWorkingCopy configuration =
          configurationType.newInstance(null, launchConfigurationName);

      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
          resource.getProjectName());
      IMethod mainMethod = resource.getMainMethod();
      if (mainMethod != null && mainMethod.exists()) {
        configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
            mainMethod.getDeclaringType().getFullyQualifiedName());
      }

      String groupIdentifier =
          mode.equals(ILaunchManager.RUN_MODE) ? IDebugUIConstants.ID_RUN_LAUNCH_GROUP
              : IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP;
      int returnStatus =
          DebugUITools.openLaunchConfigurationDialog(DataflowUiPlugin.getActiveWindowShell(),
              configuration, groupIdentifier, null);
      if (returnStatus == Window.OK) {
        configuration.doSave();
      }
    } catch (CoreException e) {
      // TODO: Handle
      DataflowUiPlugin.logError(e, "Error while launching new Launch Configuration for project %s",
          resource.getProjectName());
    }
  }

  private void launch(ILaunchConfiguration[] configurations, IResource resource, String mode) {
    if (resource == null) {
      return;
    }
    if (configurations.length == 0) {
      launchNew(toLaunchableResource(resource), mode);
    } else {
      launchExisting(configurations[0], mode);
    }
  }

  @Override
  public void launch(ISelection selection, String mode) {
    if (selection == null || selection.isEmpty()) {
      return;
    }
    launch(getLaunchConfigurations(selection), getLaunchableResource(selection), mode);
  }

  @Override
  public void launch(IEditorPart editorPart, String mode) {
    if (editorPart == null) {
      return;
    }
    launch(getLaunchConfigurations(editorPart), getLaunchableResource(editorPart), mode);
  }

  @Override
  public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
    IResource resource = getLaunchableResource(selection);
    LaunchableResource launchableResource = toLaunchableResource(resource);
    if (launchableResource == null) {
      return new ILaunchConfiguration[0];
    }
    return getResourceLaunchConfiguration(launchableResource);
  }

  @Override
  public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorPart) {
    IResource resource = getLaunchableResource(editorPart);
    LaunchableResource launchableResource = toLaunchableResource(resource);
    return getResourceLaunchConfiguration(launchableResource);
  }

  private ILaunchConfigurationType getDataflowLaunchConfigurationType(
      ILaunchManager launchManager) {
    ILaunchConfigurationType launchConfigurationType =
        launchManager.getLaunchConfigurationType(DATAFLOW_LAUNCH_CONFIGURATION_TYPE_ID);
    return launchConfigurationType;
  }

  private ILaunchConfiguration[] getDataflowLaunchConfigurations() {
    ILaunchManager launchManager = getLaunchManager();
    ILaunchConfigurationType launchConfigurationType =
        getDataflowLaunchConfigurationType(launchManager);
    try {
      return launchManager.getLaunchConfigurations(launchConfigurationType);
    } catch (CoreException e) {
      // TODO: handle
      DataflowUiPlugin.logError(e,
          "Exception while trying to retrieve Dataflow launch configurations");
    }
    return new ILaunchConfiguration[0];
  }

  private ILaunchConfiguration[] getResourceLaunchConfiguration(LaunchableResource resource) {
    ILaunchConfiguration[] dfLaunchConfigurations = getDataflowLaunchConfigurations();
    String launchConfigurationName = resource.getLaunchName();
    for (ILaunchConfiguration configuration : dfLaunchConfigurations) {
      if (configuration.getName().equals(launchConfigurationName)) {
        return new ILaunchConfiguration[] {configuration};
      }
    }
    return new ILaunchConfiguration[0];
  }

  private static LaunchableResource toLaunchableResource(IResource resource) {
    if (resource == null) {
      return null;
    }
    IJavaElement javaElement = resource.getAdapter(IJavaElement.class);
    if (javaElement != null && javaElement.exists() && javaElement instanceof ICompilationUnit) {
      ICompilationUnit compilationUnit = (ICompilationUnit) javaElement;
      IType javaType = compilationUnit.findPrimaryType();
      if (javaType == null) {
        return null;
      }
      IMethod mainMethod = javaType.getMethod(
          "main", new String[] {Signature.createTypeSignature("String[]", false)});
      return new LaunchableResource(resource, mainMethod, javaType);
    }
    return new LaunchableResource(resource);
  }

  @Override
  public IResource getLaunchableResource(ISelection selection) {
    if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      Object element = structuredSelection.getFirstElement();
      if (element instanceof IAdaptable) {
        IAdaptable adaptable = (IAdaptable) element;
        return adaptable.getAdapter(IResource.class);
      }
    }
    return null;
  }

  @Override
  public IResource getLaunchableResource(IEditorPart editorPart) {
    return editorPart.getEditorInput().getAdapter(IResource.class);
  }

  private ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }
}
