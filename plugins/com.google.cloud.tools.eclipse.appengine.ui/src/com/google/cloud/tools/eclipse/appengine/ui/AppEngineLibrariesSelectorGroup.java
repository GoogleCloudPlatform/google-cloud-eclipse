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

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class AppEngineLibrariesSelectorGroup implements ISelectionProvider {
  // TODO obtain libraries from extension registry
  // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/819
  private static Map<String, Library> getAvailableLibraries() {
    Map<String, Library> libraries = new LinkedHashMap<>();
    Library appEngine = new Library("appengine-api");
    appEngine.setName("App Engine API");
    libraries.put(appEngine.getId(), appEngine);
    Library endpoints = new Library("appengine-endpoints");
    endpoints.setName("App Engine Endpoints");
    endpoints.setLibraryDependencies(Collections.singletonList("appengine-api"));
    libraries.put(endpoints.getId(), endpoints);
    Library objectify = new Library("objectify");
    objectify.setName("Objectify");
    objectify.setLibraryDependencies(Collections.singletonList("appengine-api"));
    libraries.put(objectify.getId(), objectify);
    return libraries;
  }

  private Composite parentContainer;

  /** The libraries that can be selected. */
  private final Map<String, Library> availableLibraries;
  /** The user's explicitly selected libraries. */
  private final Collection<Library> explicitLibraries = new HashSet<>();

  private final Map<Library, Button> libraryButtons = new LinkedHashMap<>();
  private final ListenerList/* <ISelectedChangeListener> */ listeners = new ListenerList/* <> */();


  public AppEngineLibrariesSelectorGroup(Composite parentContainer) {
    Preconditions.checkNotNull(parentContainer, "parentContainer is null");
    this.parentContainer = parentContainer;
    this.availableLibraries = getAvailableLibraries();
    createContents();
  }

  private void createContents() {
    Group apiGroup = new Group(parentContainer, SWT.NONE);
    apiGroup.setText(Messages.getString("appengine.libraries.group"));
    GridDataFactory.fillDefaults().span(2, 1).applyTo(apiGroup);

    for (Library library : availableLibraries.values()) {
      Button libraryButton = new Button(apiGroup, SWT.CHECK);
      libraryButton.setText(getLibraryName(library));
      libraryButton.setData(library);
      libraryButton.addSelectionListener(new ManualSelectionTracker());
      libraryButtons.put(library, libraryButton);
    }
    GridLayoutFactory.fillDefaults().applyTo(apiGroup);
  }

  /**
   * Returns a computed value based on whether the associated library is in a list or not.
   */
  public Collection<Library> getSelectedLibraries() {
    Collection<Library> libraries = new HashSet<>(explicitLibraries);
    for (Library library : explicitLibraries) {
      for (String depId : library.getLibraryDependencies()) {
        libraries.add(availableLibraries.get(depId));
      }
    }
    return libraries;
  }

  private void updateButtons() {
    Set<Library> included = new HashSet<>(getSelectedLibraries());
    for (Entry<Library, Button> entry : libraryButtons.entrySet()) {
      Library dep = entry.getKey();
      Button button = entry.getValue();
      button.setSelection(included.contains(dep));
      button.setEnabled(!included.contains(dep) || explicitLibraries.contains(dep));
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

  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    listeners.add(listener);
  }

  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    listeners.remove(listener);
  }

  public ISelection getSelection() {
    return new StructuredSelection(getSelectedLibraries());
  }

  public void setSelection(ISelection selection) {
    throw new UnsupportedOperationException();
  }

  private void fireSelectionListeners() {
    SelectionChangedEvent event =
        new SelectionChangedEvent(this, getSelection());
    for (Object l : listeners.getListeners()) {
      ((ISelectionChangedListener) l).selectionChanged(event);
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
        explicitLibraries.add(clicked);
      } else {
        explicitLibraries.remove(clicked);
      }
      updateButtons();
      fireSelectionListeners();
    }
  }

}
