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

import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineWizardPage;
import com.google.cloud.tools.eclipse.appengine.newproject.Messages;
import com.google.cloud.tools.eclipse.appengine.newproject.maven.MavenCoordinatesWizardUi;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineRuntime;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
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
  static final AppEngineRuntime DEFAULT_RUNTIME = AppEngineRuntime.STANDARD_JAVA_8;

  private ComboViewer runtimeField;

  AppEngineStandardWizardPage() {
    super(true);
    setTitle(Messages.getString("app.engine.standard.project")); //$NON-NLS-1$
    setDescription(Messages.getString("create.app.engine.standard.project")); //$NON-NLS-1$
  }

  @Override
  protected void setHelp(Composite container) {
    PlatformUI.getWorkbench().getHelpSystem().setHelp(container,
        "com.google.cloud.tools.eclipse.appengine.newproject.NewStandardProjectContext"); //$NON-NLS-1$
  }

  @Override
  protected void createRuntimeField(Composite composite) {
    Label runtimeLabel = new Label(composite, SWT.LEAD);
    runtimeLabel.setText(Messages.getString("app.engine.standard.project.runtimetype")); //$NON-NLS-1$
    runtimeField = new ComboViewer(composite, SWT.READ_ONLY);
    runtimeField.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object element) {
        return ((AppEngineRuntime) element).getLabel();
      }
    });
    runtimeField.setContentProvider(ArrayContentProvider.getInstance());
    runtimeField.setInput(AppEngineRuntime.STANDARD_RUNTIMES);
    runtimeField.setSelection(new StructuredSelection(DEFAULT_RUNTIME), true);
    runtimeField.addPostSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        revalidate();
      }
    });
  }

  @Override
  protected String getRuntimeId() {
    AppEngineRuntime selected = DEFAULT_RUNTIME;
    if (runtimeField != null && !runtimeField.getSelection().isEmpty()) {
      Preconditions.checkState(runtimeField.getSelection() instanceof IStructuredSelection,
          "ComboViewer should return an IStructuredSelection");
      IStructuredSelection selection = (IStructuredSelection) runtimeField.getSelection();
      selected = (AppEngineRuntime) selection.getFirstElement();
    }
    return selected.getId();
  }

  @Override
  protected MavenCoordinatesWizardUi createMavenCoordinatesInput(Composite container) {
    return new MavenCoordinatesWizardUi(container, SWT.NONE);
  }
}
