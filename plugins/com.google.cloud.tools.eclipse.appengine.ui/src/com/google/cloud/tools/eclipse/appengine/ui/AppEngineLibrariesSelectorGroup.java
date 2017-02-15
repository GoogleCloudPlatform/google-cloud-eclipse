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
import com.google.cloud.tools.eclipse.ui.util.databinding.BooleanConverter;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class AppEngineLibrariesSelectorGroup {
  private DialogPage parentDialog;
  private Composite parentContainer;
  private List<Button> libraryButtons = new LinkedList<>();
  private DataBindingContext bindingContext;

  public AppEngineLibrariesSelectorGroup(DialogPage parentDialog, Composite parentContainer) {
    Preconditions.checkNotNull(parentDialog, "parentDialog is null");
    Preconditions.checkNotNull(parentContainer, "parentContainer is null");

    this.parentDialog = parentDialog;
    this.parentContainer = parentContainer;
    createContents();
  }

  public List<Library> getSelectedLibraries() {
    List<Library> selected = new LinkedList<>();
    for (Button button : libraryButtons) {
      if (button.getSelection()) {
        selected.add((Library) button.getData());
      }
    }
    return selected;
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
      libraryButtons.add(libraryButton);
    }

    addDatabindingForDependencies();

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
    return Arrays.asList(appEngine, endpoints);
  }

  private static String getLibraryName(Library library) {
    if (!Strings.isNullOrEmpty(library.getName())) {
      return library.getName();
    } else {
      return library.getId();
    }
  }

  private void addDatabindingForDependencies() {
    bindingContext = new DataBindingContext();
    for (Button libraryButton : libraryButtons) {
      Library library = (Library) libraryButton.getData();
      if (!library.getLibraryDependencies().isEmpty()) {
        addDatabindingForDependencies(libraryButton);
      }
    }
  }

  private void addDatabindingForDependencies(Button libraryButton) {
    Library library = (Library) libraryButton.getData();
    for (String libraryId : library.getLibraryDependencies()) {
      Button dependencyButton = getButtonForLibraryId(libraryId);
      if (dependencyButton != null) {
        ISWTObservableValue libraryButtonSelection = WidgetProperties.selection().observe(libraryButton);
        IObservableValue dependencyButtonSelection =
            PojoProperties.value(Button.class, "selection").observe(getDisplayRealm(), dependencyButton);
        IObservableValue dependencyButtonEnablement =
            PojoProperties.value(Button.class, "enabled").observe(getDisplayRealm(), dependencyButton);

        WritableValue intermediate = new WritableValue(false, Boolean.class);
        bindingContext.bindValue(libraryButtonSelection, intermediate);
        bindingContext.bindValue(dependencyButtonSelection, intermediate);
        bindingContext.bindValue(dependencyButtonEnablement, intermediate,
                                 new UpdateValueStrategy().setConverter(BooleanConverter.negate()),
                                 new UpdateValueStrategy().setConverter(BooleanConverter.negate()));
      }
    }
  }

  private Button getButtonForLibraryId(String libraryId) {
    for (Button button : libraryButtons) {
      Library library = (Library) button.getData();
      if (library.getId().equals(libraryId)) {
        return button;
      }
    }
    return null;
  }

  private Realm getDisplayRealm() {
    return DisplayRealm.getRealm(parentDialog.getControl().getDisplay()); 
  }

  public void dispose() {
    if (bindingContext != null) {
      bindingContext.dispose();
    }
  }

}