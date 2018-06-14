/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.util.io;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.junit.After;
import org.junit.Test;

/** Tests for the utility methods on {@link ResourceUtils}. */
public class ResourceUtilsTest {
  private IProject project;

  @After
  public void tearDown() throws CoreException {
    if (project != null) {
      project.delete(true, null);
    }
  }

  @Test
  public void testCreateFolders() throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    project = root.getProject("test-project");
    project.create(null);
    project.open(null);
    assertTrue(project.exists());

    IFolder folder = project.getFolder(new Path("foo/bar/baz"));
    assertFalse(folder.exists());
    ResourceUtils.createFolders(folder, null);
    assertTrue(folder.exists());
  }

  @Test
  public void testGetAffectedFiles_null() throws CoreException {
    Collection<IFile> files = ResourceUtils.getAffectedFiles(null);
    assertNotNull(files);
    assertEquals(0, files.size());
  }

  @Test
  public void testGetAffectedFiles_emptyDelta() throws CoreException {
    IResourceDelta delta = mockDelta();
    Collection<IFile> files = ResourceUtils.getAffectedFiles(delta);
    assertNotNull(files);
    assertEquals(0, files.size());
  }

  @Test
  public void testGetAffectedFiles_changedFile() throws CoreException {
    IFile file = mock(IFile.class, "/filename");

    IResourceDelta delta = mockDelta();
    when(delta.getResource()).thenReturn(file);
    when(delta.getKind()).thenReturn(IResourceDelta.CHANGED);
    when(delta.getFlags()).thenReturn(IResourceDelta.CONTENT);

    Collection<IFile> files = ResourceUtils.getAffectedFiles(delta);
    assertNotNull(files);
    assertEquals(1, files.size());
    assertThat(files, hasItem(file));
  }

  @Test
  public void testGetAffectedFiles_projectFolderFiles() throws CoreException {
    @SuppressWarnings("hiding") // nothing to delete
    IProject project = mock(IProject.class, "/project");

    IFolder folder = mock(IFolder.class, "/project/folder");
    when(folder.getProject()).thenReturn(project);
    when(folder.getParent()).thenReturn(project);

    IFile file1 = mock(IFile.class, "/project/folder/file1");
    when(file1.getProject()).thenReturn(project);
    when(file1.getParent()).thenReturn(folder);

    IFile file2 = mock(IFile.class, "/project/folder/file2");
    when(file2.getProject()).thenReturn(project);
    when(file2.getParent()).thenReturn(folder);

    IResourceDelta file1Delta = mockDelta();
    when(file1Delta.getResource()).thenReturn(file1);
    when(file1Delta.getKind()).thenReturn(IResourceDelta.CHANGED);
    when(file1Delta.getFlags()).thenReturn(IResourceDelta.CONTENT);

    IResourceDelta file2Delta = mockDelta();
    when(file2Delta.getResource()).thenReturn(file2);
    when(file2Delta.getKind()).thenReturn(IResourceDelta.CHANGED);
    when(file2Delta.getFlags()).thenReturn(IResourceDelta.CONTENT);

    IResourceDelta folderDelta = mockDelta();
    when(folderDelta.getResource()).thenReturn(folder);
    when(folderDelta.getKind()).thenReturn(IResourceDelta.CHANGED);
    when(folderDelta.getFlags()).thenReturn(IResourceDelta.CONTENT);
    when(folderDelta.getAffectedChildren())
        .thenReturn(new IResourceDelta[] {file1Delta, file2Delta});

    IResourceDelta projectDelta = mockDelta();
    when(projectDelta.getResource()).thenReturn(project);
    when(projectDelta.getKind()).thenReturn(IResourceDelta.CHANGED);
    when(projectDelta.getFlags()).thenReturn(IResourceDelta.CONTENT);
    when(projectDelta.getAffectedChildren()).thenReturn(new IResourceDelta[] {folderDelta});

    Collection<IFile> files = ResourceUtils.getAffectedFiles(projectDelta);
    assertNotNull(files);
    assertEquals(2, files.size());
    assertThat(files, hasItem(file1));
    assertThat(files, hasItem(file2));
  }

  private static IResourceDelta mockDelta() throws CoreException {
    // the actual ResourceDelta class includes a lot of internals
    IResourceDelta delta = mock(IResourceDelta.class);
    IResourceDelta[] empty = new IResourceDelta[0];

    when(delta.getAffectedChildren()).thenReturn(empty);
    doAnswer(
            invocation -> {
              IResourceDelta thisDelta = (IResourceDelta) invocation.getMock();
              IResourceDeltaVisitor visitor =
                  invocation.getArgumentAt(0, IResourceDeltaVisitor.class);

              if (visitor.visit(thisDelta)) {
                for (IResourceDelta childDelta : thisDelta.getAffectedChildren()) {
                  childDelta.accept(visitor);
                }
              }
              return null;
            })
        .when(delta)
        .accept(any(IResourceDeltaVisitor.class));

    return delta;
  }
}
