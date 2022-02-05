/*
 * Copyright 2019 Google LLC
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

package com.google.cloud.tools.eclipse.appengine.libraries.ui;

import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * UI to select useful libraries for GCP projects
 */
public class CloudLibrariesSelectionPage extends WizardPage {

  /**
   * The library groups to be displayed; pairs of (id, title). For example, <em>"clientapis" &rarr;
   * "Google Cloud APIs for Java"</em>.
   */
  @VisibleForTesting
  Map<String, String> libraryGroups;
  protected final List<LibrarySelectorGroup> librariesSelectors = new ArrayList<>();
  
  /** The initially selected libraries. */
  protected List<Library> initialSelection = Collections.emptyList();


  public CloudLibrariesSelectionPage() {
    super("cloudPlatformLibrariesPage"); //$NON-NLS-1$
    setTitle(Messages.getString("cloud-platform-libraries-title")); //$NON-NLS-1$
    setDescription(Messages.getString("apiclientlibrariespage-description")); //$NON-NLS-1$
    setImageDescriptor(SharedImages.GCP_WIZARD_IMAGE_DESCRIPTOR);
  }

  /**
   * Return the list of selected libraries.
   */
  public final List<Library> getSelectedLibraries() {
    List<Library> selectedLibraries = new ArrayList<>();
    for (LibrarySelectorGroup librariesSelector : librariesSelectors) {
      selectedLibraries.addAll(librariesSelector.getSelectedLibraries());
    }
    return selectedLibraries;
  }
  
  protected final void setSelectedLibraries(Collection<Library> selectedLibraries) {
    initialSelection = new ArrayList<>(selectedLibraries);
    if (!librariesSelectors.isEmpty()) {
      for (LibrarySelectorGroup librarySelector : librariesSelectors) {
        librarySelector.setSelection(new StructuredSelection(initialSelection));
      }
    }
  }

  /**
   * Set the different library groups to be shown; must be called before controls are created.
   */
  public final void setLibraryGroups(Map<String, String> groups) {
    libraryGroups = groups;
  }
  
  /**
   * The libraries that are available for selection.
   */
  protected final List<Library> getAvailableLibraries() {
    List<Library> available = new ArrayList<>();
    for (String libraryGroupId : libraryGroups.keySet()) {
      available.addAll(CloudLibraries.getLibraries(libraryGroupId));
    }
    return available;
  }
  
  @Override
  public void createControl(Composite parent) {
    createWidget(parent, false);
  }

  protected final void createWidget(Composite parent, boolean restrictedEnvironment) {
    Preconditions.checkNotNull(libraryGroups, "Library groups must be set"); //$NON-NLS-1$
    Composite composite = new Composite(parent, SWT.NONE);

    // create the library selector libraryGroups
    for (Entry<String, String> group : libraryGroups.entrySet()) {
      LibrarySelectorGroup librariesSelector =
          new LibrarySelectorGroup(
              composite, group.getKey(), group.getValue(), restrictedEnvironment);
      librariesSelectors.add(librariesSelector);
    }
    setSelectedLibraries(initialSelection);

    GridLayoutFactory.fillDefaults().numColumns(libraryGroups.size()).generateLayout(composite);

    setControl(composite);
  }

}