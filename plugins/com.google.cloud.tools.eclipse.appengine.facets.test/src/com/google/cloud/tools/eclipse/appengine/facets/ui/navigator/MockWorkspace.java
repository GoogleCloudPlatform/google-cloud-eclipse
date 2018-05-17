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

package com.google.cloud.tools.eclipse.appengine.facets.ui.navigator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.mockito.stubbing.Answer;

/** Creates an IWorkspace based on mock objects for fast and quick testing. */
public class MockWorkspace {
  // To pick up any unimplemented methods
  private static final Answer<?> unimplemented =
      invocation -> {
        throw new IllegalStateException("unimplemented");
      };

  private final IWorkspace workspace;
  private final IWorkspaceRoot workspaceRoot;
  private final Map<String, IProject> projects = new HashMap<>();
  private final Map<IContainer, Map<String, IResource>> containers = new HashMap<>();
  private final Map<IFile, byte[]> fileContents = new HashMap<>();

  public MockWorkspace() {
    workspace = mock(IWorkspace.class, withSettings().defaultAnswer(unimplemented));
    doReturn("<WORKSPACE>").when(workspace).toString();
    workspaceRoot = mock(IWorkspaceRoot.class, withSettings().defaultAnswer(unimplemented));
    doReturn("<WORKSPACE-ROOT>").when(workspaceRoot).toString();
    IResourceRuleFactory ruleFactory =
        mock(IResourceRuleFactory.class, withSettings().defaultAnswer(invocation -> workspaceRoot));
    doReturn(workspaceRoot).when(workspace).getRoot();
    doReturn(ruleFactory).when(workspace).getRuleFactory();
    configureContainer(workspaceRoot);
  }

  public IWorkspace getWorkspace() {
    return workspace;
  }

  public IWorkspaceRoot getWorkspaceRoot() {
    return workspaceRoot;
  }

  public IFacetedProject createFacetedProject(String name, IProjectFacetVersion... facetVersions)
      throws CoreException {
    IProject project = createProject(name);
    IFacetedProject facetedProject =
        mock(IFacetedProject.class, withSettings().name(name).defaultAnswer(unimplemented));
    doReturn("FP/" + name).when(facetedProject).toString();
    doReturn(project).when(facetedProject).getProject();
    doReturn(Sets.newHashSet(facetVersions)).when(facetedProject).getProjectFacets();
    doReturn(false).when(facetedProject).hasProjectFacet(any(IProjectFacetVersion.class));
    doReturn(false).when(facetedProject).hasProjectFacet(any(IProjectFacet.class));
    //    doReturn(true)
    //        .when(project)
    //        .isNatureEnabled(
    //            "org.eclipse.wst.common.project.facet.core.nature"); //
    // FacetedProjectNature.NATURE_ID;
    for (IProjectFacetVersion facetVersion : facetVersions) {
      doReturn(facetVersion)
          .when(facetedProject)
          .getProjectFacetVersion(facetVersion.getProjectFacet());
      doReturn(true).when(facetedProject).hasProjectFacet(facetVersion);
      doReturn(true).when(facetedProject).hasProjectFacet(facetVersion.getProjectFacet());
    }
    return facetedProject;
  }

  public IProject createProject(String name) throws CoreException {
    assertFalse(projects.containsKey(name));
    IProject project = mock(IProject.class, withSettings().name(name).defaultAnswer(unimplemented));
    doReturn("P/" + name).when(project).toString();
    doReturn(name).when(project).getName();
    doReturn(workspace).when(project).getWorkspace();
    doReturn(project).when(project).getProject();
    doReturn(workspaceRoot).when(project).getParent();
    doReturn(new Path(name)).when(project).getFullPath();
    doReturn(new Path(name)).when(project).getRawLocation();
    doReturn(IResource.PROJECT).when(project).getType();
    doReturn(false).when(project).isNatureEnabled(anyString());
    // isAccessible=false so ComponentCore doesn't trigger
    doReturn(false).when(project).isAccessible();
    configureContainer(project);
    projects.put(name, project);
    return project;
  }

  public IFile createFile(IProject project, IPath location, String contents) {
    return createFile(project, location, contents.getBytes(StandardCharsets.UTF_8));
  }

  public IFile createFile(IProject project, IPath location, byte[] contents) {
    assertTrue(projects.containsKey(project.getName()));
    IContainer currentDirectory = project;
    for (int i = 0; i < location.segmentCount() - 1; i++) {
      Map<String, IResource> directory = containers.get(currentDirectory);
      if (directory == null) {
        containers.put(currentDirectory, directory = new HashMap<>());
      }
      String directoryName = location.segment(i);
      assertTrue(
          !directory.containsKey(directoryName) || directory.get(directoryName) instanceof IFolder);
      if (directory.containsKey(directoryName)) {
        currentDirectory = (IFolder) directory.get(directoryName);
      } else {
        IFolder folder =
            mock(IFolder.class, withSettings().name(directoryName).defaultAnswer(unimplemented));
        doReturn(directoryName).when(folder).getName();
        doReturn(project).when(folder).getProject();
        doReturn(currentDirectory).when(folder).getParent();
        doReturn(location.uptoSegment(i)).when(folder).getLocation();
        doReturn(location.uptoSegment(i + 1).toString()).when(folder).toString();
        doReturn(true).when(folder).exists();
        configureContainer(folder);
        directory.put(directoryName, folder);
        currentDirectory = folder;
      }
    }
    Map<String, IResource> directory = containers.get(currentDirectory);
    if (directory == null) {
      containers.put(currentDirectory, directory = new HashMap<>());
    }
    assertFalse(directory.containsKey(location.lastSegment()));
    IFile file =
        mock(IFile.class, withSettings().name(location.lastSegment()).defaultAnswer(unimplemented));
    doReturn(location.toString()).when(file).toString();
    doReturn(location.lastSegment()).when(file).getName();
    doReturn(project).when(file).getProject();
    doReturn(currentDirectory).when(file).getParent();
    doReturn(location).when(file).getLocation();
    doReturn(location.toString()).when(file).toString();
    doReturn(true).when(file).exists();
    try {
      doAnswer(invocation -> new ByteArrayInputStream(fileContents.get(file)))
          .when(file)
          .getContents();
      doAnswer(
              invocation ->
                  fileContents.put(
                      file,
                      ByteStreams.toByteArray(invocation.getArgumentAt(0, InputStream.class))))
          .when(file)
          .setContents(any(InputStream.class), anyInt(), any(IProgressMonitor.class));
    } catch (CoreException ex) {
      /* never happens */
    }
    fileContents.put(file, contents);
    directory.put(location.lastSegment(), file);
    return file;
  }

  /** Configure an IContainer's getFolder() and getFile(). */
  private void configureContainer(IContainer container) {
    doAnswer(invocation -> getFile(container, invocation.getArgumentAt(0, IPath.class)))
        .when(container)
        .getFile(any(IPath.class));
    doAnswer(invocation -> getFolder(container, invocation.getArgumentAt(0, IPath.class)))
        .when(container)
        .getFolder(any(IPath.class));
    doAnswer(invocation -> container.getParent().getFullPath().append(container.getName()))
        .when(container)
        .getFullPath();
    doAnswer(invocation -> container.getParent().getRawLocation().append(container.getName()))
        .when(container)
        .getRawLocation();
    doReturn(true).when(container).exists();
    doAnswer(invocation -> resolve(container, invocation.getArgumentAt(0, IPath.class)) != null)
        .when(container)
        .exists(any(IPath.class));

    if (container instanceof IFolder) {
      IFolder folder = (IFolder) container;
      doAnswer(invocation -> getFile(folder, new Path(invocation.getArgumentAt(0, String.class))))
          .when(folder)
          .getFile(anyString());
      doAnswer(invocation -> getFolder(folder, new Path(invocation.getArgumentAt(0, String.class))))
          .when(folder)
          .getFolder(anyString());
    }
    if (container instanceof IProject) {
      IProject project = (IProject) container;
      doAnswer(invocation -> getFile(project, new Path(invocation.getArgumentAt(0, String.class))))
          .when(project)
          .getFile(anyString());
      doAnswer(
              invocation -> getFolder(project, new Path(invocation.getArgumentAt(0, String.class))))
          .when(project)
          .getFolder(anyString());
    }
  }

  private IFolder getFolder(IContainer container, IPath location) {
    IResource resolved = resolve(container, location);
    return resolved instanceof IFolder ? (IFolder) resolved : null;
  }

  private IFile getFile(IContainer container, IPath location) {
    IResource resolved = resolve(container, location);
    return resolved instanceof IFile ? (IFile) resolved : null;
  }

  private IResource resolve(IContainer container, IPath location) {
    if (container == null || container == workspaceRoot) {
      if (location.isEmpty()) {
        return workspaceRoot;
      }
      container = projects.get(location.segment(0));
      if (container == null) {
        return null;
      }
      location = location.removeFirstSegments(1);
    }
    IResource current = container;
    while (!location.isEmpty() && current instanceof IContainer) {
      Map<String, IResource> directory = containers.get(current);
      if (directory == null) {
        return null;
      }
      String name = location.segment(0);
      current = directory.get(name);
      location = location.removeFirstSegments(1);
    }
    return location.isEmpty() ? current : null;
  }
}
