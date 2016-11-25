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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

// TODO https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/911
public class AppEngineLibrariesSelectorGroup {

  private static final String BUTTON_MANUAL_SELECTION_KEY = "manualSelection";

  private Composite parentContainer;
  private final List<Button> libraryButtons;
  private DataBindingContext bindingContext;
  private final IObservableList selectedLibraries;

  public AppEngineLibrariesSelectorGroup(Composite parentContainer) {
    Preconditions.checkNotNull(parentContainer, "parentContainer is null");
    this.parentContainer = parentContainer;
    selectedLibraries = new WritableList(getDisplayRealm());
    libraryButtons = new ArrayList<>();
    createContents();
  }

  public List<Library> getSelectedLibraries() {
    return new ArrayList<>(selectedLibraries);
  }

  private void createContents() {
    Group apiGroup = new Group(parentContainer, SWT.NONE);
    apiGroup.setText(Messages.getString("appengine.libraries.group"));
    GridDataFactory.fillDefaults().span(2, 1).applyTo(apiGroup);

    List<Library> libraries = getLibraries();
    for (Library library : libraries) {
      Button libraryButton = new Button(apiGroup, SWT.CHECK);
      libraryButton.setText(getLibraryName(library));
      libraryButton.setData(library);
      libraryButton.addSelectionListener(new ManualSelectionTracker());
      libraryButtons.add(libraryButton);
    }
    setupDatabinding();
    GridLayoutFactory.fillDefaults().applyTo(apiGroup);
  }

  // TODO obtain libraries from extension registry
  // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/819
  private List<Library> getLibraries() {
    Library appEngine = new Library("appengine-api");
    appEngine.setName("App Engine API");
    Library endpoints = new Library("appengine-endpoints");
    endpoints.setName("App Engine Endpoints");
    endpoints.setLibraryDependencies(Collections.singletonList("appengine-api"));
    Library objectify = new Library("objectify");
    objectify.setName("Objectify");
    objectify.setLibraryDependencies(Collections.singletonList("appengine-api"));
    return Arrays.asList(appEngine, endpoints, objectify);
  }

  private static String getLibraryName(Library library) {
    if (!Strings.isNullOrEmpty(library.getName())) {
      return library.getName();
    } else {
      return library.getId();
    }
  }

  private void setupDatabinding() {
    bindingContext = new DataBindingContext(getDisplayRealm());
    for (Button libraryButton : libraryButtons) {
      setupDatabindingForButton(libraryButton);
    }
  }

  /**
   * We have three bindings for each library:
   * <ol>
   * <li>A one-way binding of the checkbox selection state to add or remove the corresponding
   * library from our selected-libraries list.</li>
   * <li>The opposite of the first, a one-way binding to set the checkbox selection state when the
   * corresponding library has been selected or if it is a dependency of another selected library.
   * </li>
   * <li>A one-way binding to set the checkbox enablement when the corresponding library is a
   * dependency of a selected library.</li>
   * </ol>
   * 
   * @param libraryButton to which the databinding will be configured.
   */
  private void setupDatabindingForButton(Button libraryButton) {
    Library library = (Library) libraryButton.getData();
    ISWTObservableValue libraryButtonSelection =
        WidgetProperties.selection().observe(libraryButton);
    ISWTObservableValue libraryButtonEnablement = 
        WidgetProperties.enabled().observe(libraryButton);
    // library selection UI -> model
    bindingContext.bindValue(libraryButtonSelection, new NullComputedValue(getDisplayRealm()),
        new UpdateValueStrategy()
            .setConverter(new HandleLibrarySelectionConverter(selectedLibraries, library)),
        new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER));
    // UI <- library selection model
    boolean resultIfFound = true;
    bindingContext.bindValue(libraryButtonSelection,
        new LibrarySelected(getDisplayRealm(), library.getId(), resultIfFound, libraryButton), 
        new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER), new UpdateValueStrategy());
    // UI enablement <- library is a dependency
    bindingContext.bindValue(libraryButtonEnablement,
        new DependentLibrarySelected(getDisplayRealm(), library.getId(), false /* resultIfFound*/),
        new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER), new UpdateValueStrategy());
  }

  public void dispose() {
    if (bindingContext != null) {
      bindingContext.dispose();
    }
  }

  private Realm getDisplayRealm() {
    return DisplayRealm.getRealm(parentContainer.getDisplay());
  }

  @VisibleForTesting
  List<Button> getLibraryButtons() {
    return libraryButtons;
  }

  private final class LibrarySelected extends DependentLibrarySelected {
    private final Button libraryButton;

    private LibrarySelected(Realm realm, String libraryId, boolean resultIfFound,
        Button libraryButton) {
      super(realm, libraryId, resultIfFound);
      this.libraryButton = libraryButton;
    }

    @Override
    protected Object calculate() {
      // must call super.calculate to ensure
      // databinding will call this method when
      // selectedLibraries changes
      return (boolean) super.calculate()
          || libraryButton.getData(BUTTON_MANUAL_SELECTION_KEY) != null;
    }
  }

  /**
   * Tracks if the checkbox has been explicitly clicked by the user.
   */
  private static final class ManualSelectionTracker implements SelectionListener {
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
      if ((button.getStyle() & SWT.CHECK) != 0) {
        button.setData(BUTTON_MANUAL_SELECTION_KEY, button.getSelection() ? new Object() : null);
      }
    }
  }

  /**
   * Returns a computed value based on whether the associated library is in a list or not.
   */
  private class DependentLibrarySelected extends ComputedValue {
    private String libraryId;
    private boolean resultIfFound;

    /**
     * @param libraryId the id of the library to be searched for
     * @param resultIfFound value returned by {@link #calculate()} if the library is found
     */
    private DependentLibrarySelected(Realm realm,
                                     String libraryId,
                                     final boolean resultIfFound) {
      super(realm);
      Preconditions.checkNotNull(libraryId);
      this.resultIfFound = resultIfFound;
      this.libraryId = libraryId;
    }

    @Override
    protected Object calculate() {
      for (Object object : selectedLibraries) {
        Library library = (Library) object;
        for (String depId : library.getLibraryDependencies()) {
          if (libraryId.equals(depId)) {
            return resultIfFound;
          }
        }
      }
      return !resultIfFound;
    }
  }

  /**
   * Always returns null. Can be used in databinding if the actual value is not important;
   * i.e. converters and/or validators are used to implement the desired behavior.
   */
  private static final class NullComputedValue extends ComputedValue {

    public NullComputedValue(Realm realm) {
      super(realm);
    }

    @Override
    protected Object calculate() {
      return null;
    }
  }

  /**
   * Adds/removes the library to the list of <code>libraries</code> depending upon the boolean 
   * received for conversion. If the value is <code>true</code> it will add, otherwise it will
   * remove the library from the list.
   */
  private static final class HandleLibrarySelectionConverter extends Converter {

    private Library library;
    private List<Library> libraries;

    HandleLibrarySelectionConverter(List<Library> libraries, Library library) {
      super(Boolean.class, List.class);
      Preconditions.checkNotNull(libraries, "selector is null");
      Preconditions.checkNotNull(library, "library is null");
      this.libraries = libraries;
      this.library = library;
    }

    @Override
    public Object convert(Object fromObject) {
      Preconditions.checkArgument(fromObject instanceof Boolean);
      Boolean selected = (Boolean) fromObject;
      if (selected) {
        libraries.add(library);
      } else {
        libraries.remove(library);
      }
      return libraries;
    }

  }
}
