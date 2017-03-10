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

package com.google.cloud.tools.eclipse.appengine.ui;

import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class AppEngineLibrariesSelectorGroup implements ISelectionProvider {

  /** The libraries that can be selected. */
  private final Map<String, Library> availableLibraries;

  /** The user's explicitly selected libraries. */
  private final Collection<Library> explicitSelectedLibraries = new HashSet<>();

  private final Map<Library, Button> libraryButtons = new LinkedHashMap<>();
  private final ListenerList/* <ISelectedChangeListener> */ listeners = new ListenerList/* <> */();

  public AppEngineLibrariesSelectorGroup(Composite parentContainer) {
    Collection<Library> availableLibraries =
        CloudLibraries.getLibraries(CloudLibraries.APP_ENGINE_GROUP);
    Preconditions.checkNotNull(parentContainer, "parentContainer is null");
    Preconditions.checkNotNull(availableLibraries, "availableLibraries is null");
    
    this.availableLibraries = new LinkedHashMap<>();
    for (Library library : availableLibraries) {
      this.availableLibraries.put(library.getId(), library);
    }
    createContents(parentContainer);
  }

  private void createContents(Composite parentContainer) {
    Group apiGroup = new Group(parentContainer, SWT.NONE);
    apiGroup.setText(Messages.getString("appengine.libraries.group"));
    GridDataFactory.fillDefaults().span(2, 1).applyTo(apiGroup);

    for (Library library : availableLibraries.values()) {
      Button libraryButton = new Button(apiGroup, SWT.CHECK);
      libraryButton.setText(getLibraryName(library));
      if (library.getToolTip() != null) {
        libraryButton.setToolTipText(library.getToolTip());
      }
      libraryButton.setData(library);
      libraryButton.addSelectionListener(new ManualSelectionTracker());
      libraryButtons.put(library, libraryButton);
    }
    GridLayoutFactory.fillDefaults().applyTo(apiGroup);
  }

  /**
   * Returns the selected libraries and required dependencies.
   */
  public Collection<Library> getSelectedLibraries() {
    Collection<Library> libraries = new HashSet<>(explicitSelectedLibraries);
    libraries.addAll(getLibraryDependencies());
    return libraries;
  }

  private Collection<Library> getLibraryDependencies() {
    Collection<Library> dependencies = new HashSet<>();
    for (Library library : explicitSelectedLibraries) {
      for (String depId : library.getLibraryDependencies()) {
        dependencies.add(availableLibraries.get(depId));
      }
    }
    return dependencies;
  }

  private void updateButtons() {
    Collection<Library> dependencies = getLibraryDependencies();
    for (Entry<Library, Button> entry : libraryButtons.entrySet()) {
      Library thisLibrary = entry.getKey();
      Button button = entry.getValue();
      boolean shouldCheck =
          explicitSelectedLibraries.contains(thisLibrary) || dependencies.contains(thisLibrary);
      button.setSelection(shouldCheck);
      boolean forcedDependency = dependencies.contains(thisLibrary);
      button.setEnabled(!forcedDependency);
    }
  }

  private static String getLibraryName(Library library) {
    if (!Strings.isNullOrEmpty(library.getName())) {
      return library.getName();
    } else {
      return library.getId();
    }
  }

  public void dispose() {
  }

  @VisibleForTesting
  List<Button> getLibraryButtons() {
    return new ArrayList<>(libraryButtons.values());
  }

  @Override
  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    listeners.remove(listener);
  }

  /**
   * Return the list of selected libraries and their required libraries.
   */
  @Override
  public ISelection getSelection() {
    return new StructuredSelection(getSelectedLibraries());
  }

  /**
   * Set selection by providing a collection of {@linkplain Library} objects or
   * {@link Library#getId() library IDs}. All libraries are marked as being explicitly selected.
   * Must be called from the SWT thread.
   */
  @Override
  public void setSelection(ISelection selection) {
    explicitSelectedLibraries.clear();
    if (selection instanceof IStructuredSelection) {
      for (Object object : ((IStructuredSelection) selection).toArray()) {
        if (object instanceof String && availableLibraries.containsKey(object)) {
          explicitSelectedLibraries.add(availableLibraries.get(object));
        } else if (object instanceof Library && availableLibraries.containsValue(object)) {
          explicitSelectedLibraries.add((Library) object);
        }
      }
    }
    updateButtons();
  }

  private void fireSelectionListeners() {
    SelectionChangedEvent event =
        new SelectionChangedEvent(this, getSelection());
    for (Object listener : listeners.getListeners()) {
      ((ISelectionChangedListener) listener).selectionChanged(event);
    }
  }

  /**
   * Tracks when the checkbox has been explicitly clicked by the user.
   */
  private final class ManualSelectionTracker implements SelectionListener {
    @Override
    public void widgetSelected(SelectionEvent event) {
      setManualSelection(event);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
      setManualSelection(event);
    }

    private void setManualSelection(SelectionEvent event) {
      Preconditions.checkArgument(event.getSource() instanceof Button);

      Button button = (Button) event.getSource();
      Library clicked = (Library) button.getData();
      if (button.getSelection()) {
        explicitSelectedLibraries.add(clicked);
      } else {
        explicitSelectedLibraries.remove(clicked);
      }
      updateButtons();
      fireSelectionListeners();
    }
  }
}
