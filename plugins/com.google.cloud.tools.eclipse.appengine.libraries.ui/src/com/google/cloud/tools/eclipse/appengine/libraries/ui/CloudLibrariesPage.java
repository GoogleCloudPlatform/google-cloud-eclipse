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

package com.google.cloud.tools.eclipse.appengine.libraries.ui;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.libraries.BuildPath;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

public abstract class CloudLibrariesPage extends WizardPage implements IClasspathContainerPage,
    IClasspathContainerPageExtension {

  private static final Logger logger = Logger.getLogger(CloudLibrariesPage.class.getName());
  private final LibraryClasspathContainerSerializer serializer =
      new LibraryClasspathContainerSerializer();
  private Map<String, String> groups; // id -> title
  private List<String> initialLibraryIds = Collections.emptyList();

  private List<LibrarySelectorGroup> librariesSelectors;
  private IJavaProject project;
  private boolean isMavenProject;
  private IClasspathEntry newEntry;


  protected CloudLibrariesPage(String pageId) {
    super(pageId); // $NON-NLS-1$
  }

  /** Set the visible library groups. Map is set of (id, title) pairs. */
  protected void setGroups(Map<String, String> groups) {
    this.groups = groups;
  }

  @Override
  public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
    this.project = project;
    isMavenProject = MavenUtils.hasMavenNature(project.getProject());
  }

  @Override
  public void createControl(Composite parent) {
    Preconditions.checkNotNull(groups, "Groups must be set");
    Composite composite = new Group(parent, SWT.NONE);

    boolean java7AppEngineStandardProject = false;
    IProjectFacetVersion facetVersion =
        AppEngineStandardFacet.getProjectFacetVersion(project.getProject());
    if (facetVersion != null && facetVersion.getVersionString().equals("JRE7")) {
      java7AppEngineStandardProject = true;
    }

    // FIXME: what should happen with libraries that aren't visible in any of the groups?
    librariesSelectors = new ArrayList<>();
    for (Entry<String, String> group : groups.entrySet()) {
      LibrarySelectorGroup librariesSelector =
          new LibrarySelectorGroup(composite, group.getKey(), group.getValue(),
              java7AppEngineStandardProject);
      // setSelection() ignores any libraries not part of its group
      librariesSelector.setSelection(new StructuredSelection(initialLibraryIds));
      librariesSelectors.add(librariesSelector);
    }
    composite.setLayout(new RowLayout(SWT.HORIZONTAL));
    setControl(composite);
  }

  @Override
  public boolean finish() {
    final List<Library> libraries = new ArrayList<>();
    final List<String> libraryIds = new ArrayList<>();
    for (LibrarySelectorGroup librariesSelector : librariesSelectors) {
      libraries.addAll(librariesSelector.getSelectedLibraries());
    }
    for (Library library : libraries) {
      libraryIds.add(library.getId());
    }
    try {
      getContainer().run(false, true, new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
          try {
            if (isMavenProject) {
              BuildPath.addMavenLibraries(project.getProject(), libraries, monitor);
            } else {
              SubMonitor progress = SubMonitor.convert(monitor, 10);
              Library masterLibrary =
                  BuildPath.collectLibraryFiles(project, libraries, progress.newChild(5));
              newEntry = BuildPath.listNativeLibrary(project, masterLibrary, progress.newChild(5));
              serializer.saveLibraryIds(project, libraryIds /* , newEntry.getPath() */);
            }
          } catch (CoreException | IOException ex) {
            StatusUtil.setErrorStatus(this, "Error updating container definition", ex);
            throw new InvocationTargetException(ex);
          }
        }
      });
      return true;
    } catch (InvocationTargetException ex) {
      // ignored: exceptions are already reported above
    } catch (InterruptedException ex) {
      Thread.interrupted();
    }
    return false;
  }

  @Override
  public IClasspathEntry getSelection() {
    return newEntry;
  }

  @Override
  public void setSelection(IClasspathEntry containerEntry) {
    if (containerEntry != null) {
      try {
        initialLibraryIds = serializer.loadLibraryIds(project, containerEntry.getPath());
      } catch (CoreException | IOException ex) {
        logger.log(Level.WARNING,
            "Error loading selected library IDs for " + project.getElementName(), ex);
      }
    }
  }
}
