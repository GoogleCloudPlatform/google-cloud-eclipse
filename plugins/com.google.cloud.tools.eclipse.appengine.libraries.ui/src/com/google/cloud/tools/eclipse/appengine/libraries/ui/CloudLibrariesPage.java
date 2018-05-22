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
import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainer;
import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
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

  private IClasspathEntry originalEntry;
  private IClasspathEntry newEntry;

  public CloudLibrariesPage() {
    super("cloudPlatformLibrariesPage"); //$NON-NLS-1$
    setTitle(Messages.getString("cloud-platform-libraries-title")); //$NON-NLS-1$
    setDescription(Messages.getString("apiclientlibrariespage-description")); //$NON-NLS-1$
    setImageDescriptor(SharedImages.GCP_WIZARD_IMAGE_DESCRIPTOR);
  }

  @Override
  public void initialize(IJavaProject javaProject, IClasspathEntry[] currentEntries) {
    this.project = javaProject;
    isMavenProject = MavenUtils.hasMavenNature(javaProject.getProject());

    // As we don't support multiple containers, we pretend to edit the existing container if
    // present and cause getSelection() to return null
    Stream<IClasspathEntry> entries =
        currentEntries != null ? Stream.of(currentEntries) : Stream.empty();
    originalEntry = entries.filter(LibraryClasspathContainer::isEntry).findAny().orElse(null);

    Map<String, String> groups = new LinkedHashMap<>();
    if (AppEngineStandardFacet.getProjectFacetVersion(javaProject.getProject()) != null) {
      groups.put(CloudLibraries.APP_ENGINE_STANDARD_GROUP, 
          Messages.getString("appengine-title")); //$NON-NLS-1$
    }
    groups.put(CloudLibraries.CLIENT_APIS_GROUP, 
        Messages.getString("clientapis-title")); //$NON-NLS-1$
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
    Composite composite = new Composite(parent, SWT.NONE);

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

    GridLayoutFactory.fillDefaults().numColumns(libraryGroups.size()).generateLayout(composite);

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
        BuildPath.saveLibraryList(project, libraries, new NullProgressMonitor());
        Library masterLibrary =
            BuildPath.collectLibraryFiles(project, libraries, new NullProgressMonitor());
        // skip computeEntry() if we have an existing entry: unnecessary and simplifies testing too
        if (originalEntry == null) {
          newEntry = BuildPath.computeEntry(project, masterLibrary, new NullProgressMonitor());
          Verify.verifyNotNull(newEntry); // new entry should be created
        } else {
          // request update of existing entry
          ClasspathContainerInitializer initializer =
              JavaCore.getClasspathContainerInitializer(
                  LibraryClasspathContainer.CONTAINER_PATH_PREFIX);
          // this is always true for our initializer
          if (initializer.canUpdateClasspathContainer(originalEntry.getPath(), project)) {
            // existing entry needs to be updated
            initializer.requestClasspathContainerUpdate(
                originalEntry.getPath(), project, null /*containerSuggestion*/);
          }
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
  public void setSelection(IClasspathEntry entry) {
    // entry == null if user is creating a new container.
    Preconditions.checkArgument(entry == null || LibraryClasspathContainer.isEntry(entry));
    // we should have found the existing entry already in initialize()
    Preconditions.checkState(entry == null || entry == originalEntry);

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
    // newEntry is null if no container was created, namely because there was an existing container
    return newEntry;
  }
}
