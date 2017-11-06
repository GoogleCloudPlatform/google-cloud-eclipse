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
import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * Provides for selecting from a suite of useful libraries for GCP projects. Supports editing the
 * list of libraries for native Cloud Tools for Eclipse projects. Supported only selecting new
 * libraries for Maven-based projects.
 */
public class CloudLibrariesPage extends WizardPage
    implements IClasspathContainerPage, IClasspathContainerPageExtension {
  private static final Logger logger = Logger.getLogger(CloudLibrariesPage.class.getName());

  /**
   * The library groups to be displayed; pairs of (id, title). For example, <em>"clientapis" &rarr;
   * "Google Cloud APIs for Java"</em>.
   */
  private Map<String, String> libraryGroups;

  /** The initially selected libraries. */
  private List<Library> initialSelection = Collections.emptyList();

  private final List<LibrarySelectorGroup> librariesSelectors = new ArrayList<>();
  private IJavaProject project;
  private boolean isMavenProject;
  private IClasspathEntry oldEntry;
  private IClasspathEntry newEntry;

  public CloudLibrariesPage() {
    super(CloudLibraries.CLIENT_APIS_GROUP);
    setTitle(Messages.getString("clientapis-title")); //$NON-NLS-1$
    setDescription(Messages.getString("apiclientlibrariespage-description")); //$NON-NLS-1$
    setImageDescriptor(SharedImages.GCP_WIZARD_IMAGE_DESCRIPTOR);
  }

  @Override
  public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
    this.project = project;
    isMavenProject = MavenUtils.hasMavenNature(project.getProject());

    Map<String, String> groups = Maps.newLinkedHashMap();
    if (AppEngineStandardFacet.getProjectFacetVersion(project.getProject()) != null) {
      groups.put(CloudLibraries.APP_ENGINE_GROUP, Messages.getString("appengine-title"));
    }
    groups.put(CloudLibraries.CLIENT_APIS_GROUP, Messages.getString("clientapis-title"));
    this.libraryGroups = groups;
  }

  @Override
  public void createControl(Composite parent) {
    Preconditions.checkNotNull(libraryGroups, "Library groups must be set"); //$NON-NLS-1$
    Composite composite = new Group(parent, SWT.NONE);

    IProjectFacetVersion facetVersion =
        AppEngineStandardFacet.getProjectFacetVersion(project.getProject());
    boolean java7AppEngineStandardProject = AppEngineStandardFacet.JRE7.equals(facetVersion);

    // create the library selector libraryGroups
    for (Entry<String, String> group : libraryGroups.entrySet()) {
      LibrarySelectorGroup librariesSelector =
          new LibrarySelectorGroup(composite, group.getKey(), group.getValue(),
              java7AppEngineStandardProject);
      librariesSelectors.add(librariesSelector);
    }
    setSelectedLibraries(initialSelection);
    composite.setLayout(new RowLayout(SWT.HORIZONTAL));
    setControl(composite);
  }

  @Override
  public boolean finish() {
    final List<Library> libraries = getSelectedLibraries();
    try {
      if (isMavenProject) {
        BuildPath.addMavenLibraries(project.getProject(), libraries, new NullProgressMonitor());
      } else {
        /*
         * FIXME: BuildPath.addNativeLibrary() is too heavy-weight here. ClasspathContainerWizard,
         * our wizard, is responsible for installing the classpath entry returned by getSelection(),
         * which will perform the library resolution. We just need to save the selected libraries 
         * so that they are resolved later.
         */
        Library masterLibrary =
            BuildPath.collectLibraryFiles(project, libraries, new NullProgressMonitor());
        newEntry = BuildPath.computeEntry(project, masterLibrary, new NullProgressMonitor());
        BuildPath.saveLibraryList(project, libraries, new NullProgressMonitor());
        if (newEntry == null) {
          // container-editing only refreshes the content if new
          BuildPath.runContainerResolverJob(project);
        }
      }
      return true;
    } catch (CoreException ex) {
      StatusUtil.setErrorStatus(this, "Error updating container definition", ex); //$NON-NLS-1$
      return false;
    }
  }

  @VisibleForTesting
  List<Library> getVisibleLibraries() {
    List<Library> visible = new ArrayList<>();
    for (LibrarySelectorGroup librariesSelector : librariesSelectors) {
      visible.addAll(librariesSelector.getAvailableLibraries());
    }
    return visible;
  }

  /**
   * Return the list of selected libraries.
   */
  @VisibleForTesting
  List<Library> getSelectedLibraries() {
    List<Library> selectedLibraries = new ArrayList<>(initialSelection);
    for (LibrarySelectorGroup librariesSelector : librariesSelectors) {
      selectedLibraries.addAll(librariesSelector.getSelectedLibraries());
    }
    return selectedLibraries;
  }

  @VisibleForTesting
  void setSelectedLibraries(List<Library> selectedLibraries) {
    initialSelection = new ArrayList<>(selectedLibraries);
    List<Library> remaining = new ArrayList<>(selectedLibraries);
    if (!librariesSelectors.isEmpty()) {
      for (LibrarySelectorGroup librarySelector : librariesSelectors) {
        librarySelector.setSelection(new StructuredSelection(initialSelection));
        remaining.removeAll(librarySelector.getSelectedLibraries());
      }
      if (!remaining.isEmpty()) {
        logger.log(Level.WARNING,
            "Discarding libraries that aren't availble from library container definition: " //$NON-NLS-1$
                + remaining);
      }
    }
  }

  @Override
  public void setSelection(IClasspathEntry containerEntry) {
    // null means new entry, so nothing to read in; might be usful to read in
    // current dependencies for maven projects
    oldEntry = containerEntry;
    if (containerEntry == null || isMavenProject) {
      return;
    }
    try {
      List<Library> savedLibraries =
          BuildPath.loadLibraryList(project, new NullProgressMonitor());
      setSelectedLibraries(savedLibraries);
    } catch (CoreException ex) {
      logger.log(Level.WARNING,
          "Error loading selected library IDs for " + project.getElementName(), ex); //$NON-NLS-1$
    }
  }

  @Override
  public IClasspathEntry getSelection() {
    return newEntry;
  }
}
