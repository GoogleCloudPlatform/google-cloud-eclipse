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

package com.google.cloud.tools.eclipse.appengine.newproject.standard;

import com.google.cloud.tools.eclipse.appengine.libraries.ILibraryClasspathContainerResolverService.AppEngineRuntime;
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineWizardPage;
import com.google.cloud.tools.eclipse.appengine.newproject.Messages;
import com.google.cloud.tools.eclipse.appengine.newproject.PageValidator;
import com.google.common.annotations.VisibleForTesting;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

public class AppEngineStandardWizardPage extends AppEngineWizardPage {
  /** The default AppEngine runtime for new projects. */
  @VisibleForTesting
  static final AppEngineRuntime DEFAULT_RUNTIME = AppEngineRuntime.STANDARD_JAVA_7;

  private ComboViewer runtimeField;

  public AppEngineStandardWizardPage() {
    super(true);
    setTitle(Messages.getString("app.engine.standard.project")); //$NON-NLS-1$
    setDescription(Messages.getString("create.app.engine.standard.project")); //$NON-NLS-1$
  }

  @Override
  public void setHelp(Composite container) {
    PlatformUI.getWorkbench().getHelpSystem().setHelp(container,
        "com.google.cloud.tools.eclipse.appengine.newproject.NewStandardProjectContext"); //$NON-NLS-1$
  }


  @Override
  protected void createRuntimeField(Composite composite, final PageValidator pageValidator) {
    Label runtimeLabel = new Label(composite, SWT.LEAD);
    runtimeLabel.setText(Messages.getString("runtime")); //$NON-NLS-1$
    runtimeField = new ComboViewer(composite, SWT.READ_ONLY);
    runtimeField.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object element) {
        return ((AppEngineRuntime) element).getLabel();
      }
    });
    runtimeField.setContentProvider(ArrayContentProvider.getInstance());
    runtimeField.setInput(AppEngineRuntime.values());
    runtimeField.setSelection(new StructuredSelection(DEFAULT_RUNTIME), true);
    runtimeField.addPostSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        pageValidator.run();
      }
    });
  }

  public String getRuntime() {
    ISelection selection = runtimeField == null ? new StructuredSelection(DEFAULT_RUNTIME)
        : runtimeField.getSelection();
    AppEngineRuntime selected = selection instanceof IStructuredSelection
        ? (AppEngineRuntime) ((IStructuredSelection) selection).getFirstElement()
        : DEFAULT_RUNTIME;
    return selected.getId();
  }
}
