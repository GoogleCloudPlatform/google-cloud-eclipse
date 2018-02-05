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
import com.google.cloud.tools.eclipse.appengine.libraries.AnalyticsLibraryPingHelper;
import com.google.cloud.tools.eclipse.appengine.libraries.BuildPath;
import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
  @VisibleForTesting
  Map<String, String> libraryGroups;

  /** The initially selected libraries. */
  private List<Library> initialSelection = Collections.emptyList();

  @VisibleForTesting
  final List<LibrarySelectorGroup> librariesSelectors = new ArrayList<>();
  private IJavaProject project;
  private boolean isMavenProject;
  private IClasspathEntry newEntry;

  public CloudLibrariesPage() {
    super("cloudPlatformLibrariesPage"); //$NON-NLS-1$
    setTitle(Messages.getString("cloud-platform-libraries-title")); //$NON-NLS-1$
    setDescription(Messages.getString("apiclientlibrariespage-description")); //$NON-NLS-1$
    setImageDescriptor(SharedImages.GCP_WIZARD_IMAGE_DESCRIPTOR);
  }

  @Override
  public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
    this.project = project;
    isMavenProject = MavenUtils.hasMavenNature(project.getProject());

    Map<String, String> groups = Maps.newLinkedHashMap();
    if (AppEngineStandardFacet.getProjectFacetVersion(project.getProject()) != null) {
      groups.put(CloudLibraries.APP_ENGINE_GROUP, Messages.getString("appengine-title")); //$NON-NLS-1$
    }
    groups.put(CloudLibraries.CLIENT_APIS_GROUP, Messages.getString("clientapis-title")); //$NON-NLS-1$
    setLibraryGroups(groups);
  }

  /**
   * Set the different library groups to be shown; must be called before controls are created.
   */
  @VisibleForTesting
  void setLibraryGroups(Map<String, String> groups) {
    libraryGroups = groups;
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
    RowLayout layout = new RowLayout(SWT.HORIZONTAL);
    layout.spacing = 12;
    composite.setLayout(layout);
    setControl(composite);
  }

  @Override
  public boolean finish() {
    List<Library> libraries = getSelectedLibraries();
    try {
      if (isMavenProject) {
        // remove any library that wasn't selected
        Set<Library> removed = new HashSet<>(getAvailableLibraries());
        removed.removeAll(libraries);
        // No need for an Analytics ping here; addMavenLibraries will do it.
        BuildPath.updateMavenLibraries(project.getProject(), libraries, removed,
            new NullProgressMonitor());
      } else {
        if (!libraries.isEmpty()) {
          AnalyticsLibraryPingHelper.sendLibrarySelectionPing(
              AnalyticsEvents.NATIVE_PROJECT, libraries);
        }

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

  /**
   * Return the list of selected libraries.
   */
  @VisibleForTesting
  List<Library> getSelectedLibraries() {
    List<Library> selectedLibraries = new ArrayList<>();
    for (LibrarySelectorGroup librariesSelector : librariesSelectors) {
      selectedLibraries.addAll(librariesSelector.getSelectedLibraries());
    }
    return selectedLibraries;
  }

  @VisibleForTesting
  void setSelectedLibraries(Collection<Library> selectedLibraries) {
    initialSelection = new ArrayList<>(selectedLibraries);
    if (!librariesSelectors.isEmpty()) {
      for (LibrarySelectorGroup librarySelector : librariesSelectors) {
        librarySelector.setSelection(new StructuredSelection(initialSelection));
      }
    }
  }

  @Override
  public void setSelection(IClasspathEntry containerEntry) {
    try {
      Collection<Library> savedLibraries;
      if (isMavenProject) {
        // must pass in available libraries for filtering unrelated dependencies
        savedLibraries = BuildPath.loadMavenLibraries(project, getAvailableLibraries(),
            new NullProgressMonitor());
      } else {
        savedLibraries = BuildPath.loadLibraryList(project, new NullProgressMonitor());
      }
      setSelectedLibraries(savedLibraries);
    } catch (CoreException ex) {
      logger.log(Level.WARNING,
          "Error loading selected library IDs for " + project.getElementName(), ex); //$NON-NLS-1$
    }
  }

  /**
   * The libraries that are available for selection.
   */
  private List<Library> getAvailableLibraries() {
    List<Library> available = new ArrayList<>();
    for (String libraryGroupId : libraryGroups.keySet()) {
      available.addAll(CloudLibraries.getLibraries(libraryGroupId));
    }
    return available;
  }

  @Override
  public IClasspathEntry getSelection() {
    return newEntry;
  }
}
