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
import java.util.LinkedList;
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
    libraryButtons = new LinkedList<>();
    createContents();
  }

  public List<Library> getSelectedLibraries() {
    return new ArrayList<>(selectedLibraries);
  }

  private void createContents() {
    Group apiGroup = new Group(parentContainer, SWT.NONE);
    apiGroup.setText(Messages.AppEngineLibrariesSelectorGroupLabel);
    GridDataFactory.fillDefaults().span(2, 1).applyTo(apiGroup);

    List<Library> libraries = getLibraries();
    for (Library library : libraries) {
      Button libraryButton = new Button(apiGroup, SWT.CHECK);
      libraryButton.setText(getLibraryName(library));
      libraryButton.setData(library);
      libraryButton.addSelectionListener(new ManualSelectionTracker());
      libraryButtons.add(libraryButton);
    }
    addDatabinding();
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

  private void addDatabinding() {
    bindingContext = new DataBindingContext(getDisplayRealm());
    for (Button libraryButton : libraryButtons) {
      addDatabindingForButton(libraryButton);
    }
  }

  private void addDatabindingForButton(final Button libraryButton) {
    final Library library = (Library) libraryButton.getData();
    ISWTObservableValue libraryButtonSelection = WidgetProperties.selection().observe(libraryButton);
    ISWTObservableValue libraryButtonEnablement = WidgetProperties.enabled().observe(libraryButton);
    // library selection UI -> model
    bindingContext.bindValue(libraryButtonSelection,
                             new NullComputedValue(getDisplayRealm()),
                             new UpdateValueStrategy().setConverter(new HandleLibrarySelectionConverter(selectedLibraries,
                                                                                                        library)),
                             new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER));
    // library selection model -> UI
    bindingContext.bindValue(libraryButtonSelection, 
                             new DependentLibrarySelected(getDisplayRealm(),
                                                          selectedLibraries,
                                                          library.getId(),
                                                          true /* resultIfFound */,
                                                          new ButtonManuallySelected(libraryButton)),
                             new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER),
                             new UpdateValueStrategy());
    // library checkbox enablement model -> UI
    bindingContext.bindValue(libraryButtonEnablement,
                             new DependentLibrarySelected(getDisplayRealm(),
                                                          selectedLibraries,
                                                          library.getId(),
                                                          false /* resultIfFound */),
                             new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER),
                             new UpdateValueStrategy());
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

  /**
   * Utility interface to return a value;
   */
  private static interface Getter<T> {
    T get();
  }

  private static final class ButtonManuallySelected implements Getter<Boolean> {
    private final Button libraryButton;

    private ButtonManuallySelected(Button libraryButton) {
      this.libraryButton = libraryButton;
    }

    /**
     * Returns true if the checkbox associated with this instance was explicitly clicked by the user.
     */
    @Override
    public Boolean get() {
      return libraryButton.getData(BUTTON_MANUAL_SELECTION_KEY) != null;
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
      Button source = (Button) event.getSource();
      if (event.getSource() instanceof Button && (source.getStyle() & SWT.CHECK) != 0) {
        Button button = source;
        button.setData(BUTTON_MANUAL_SELECTION_KEY, button.getSelection() ? new Object() : null);
      }
    }
  }

  /**
   * Returns a computed value based on whether the associated library is in a list or not. 
   */
  private static final class DependentLibrarySelected extends ComputedValue {
    private String libraryId;
    private IObservableList libraries;
    private Getter<Boolean> condition;
    private boolean selectedResult;

    /**
     * @param libraries the list of libraries to search
     * @param libraryId the id of the library to be searched for
     * @param resultIfFound value returned by {@link #calculate()} if the library is found
     */
    private DependentLibrarySelected(Realm realm,
                                     IObservableList libraries,
                                     String libraryId,
                                     final boolean resultIfFound) {
      this(realm, libraries, libraryId, resultIfFound, new Getter<Boolean>() {
        @Override
        public Boolean get() {
          return !resultIfFound;
        }});
    }

    /**
     * @param libraries the list of libraries to search
     * @param libraryId the id of the library to be searched for
     * @param resultIfFound value returned by {@link #calculate()} if the library is found
     * @param resultIfNotFound if the library is not found in the list, return the result of
     * <code>resultIfNotFound.get()</code>
     */
    private DependentLibrarySelected(Realm realm,
                                     IObservableList libraries,
                                     String libraryId,
                                     boolean resultIfFound,
                                     Getter<Boolean> resultIfNotFound) {
      super(realm);
      Preconditions.checkNotNull(libraries);
      Preconditions.checkNotNull(libraryId);
      Preconditions.checkNotNull(resultIfNotFound);
      this.selectedResult = resultIfFound;
      this.libraries = libraries;
      this.libraryId = libraryId;
      this.condition = resultIfNotFound;
    }

    @Override
    protected Object calculate() {
      for (Object object : libraries) {
        Library library = (Library) object;
        for (String depId : library.getLibraryDependencies()) {
          if (libraryId.equals(depId)) {
            return selectedResult;
          }
        }
      }
      return condition.get();
    }
  }

  /**
   * Returns null always, can be used in databinding if the actual value is not important, i.e. converters and/or
   * validators are used to implement the desired behavior.
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
   * Adds/removes the library to the list of <code>libraries</code> depending upon the boolean value received for
   * conversion. If the value is <code>true</code> it will add, otherwise it will remove the library from the list.
   */
  private static final class HandleLibrarySelectionConverter extends Converter {

    private Library library;
    private List<Library> libraries;

    public HandleLibrarySelectionConverter(List<Library> libraries, Library library) {
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
