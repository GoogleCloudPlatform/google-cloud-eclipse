/*
 * Copyright ${year} Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.eclipse.preferences.areas;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Wrap an existing {@link FieldEditor}
 *
 * @param <FE>
 * 
 *        TODO: rewrite ProperyChangeEvents for {@link FieldEditor#IS_VALID} and
 *        {@link FieldEditor#VALUE}
 */
public abstract class FieldEditorWrapper<FE extends FieldEditor> extends PreferenceArea
    implements IExecutableExtension {
  private IConfigurationElement configElement;
  private FE fieldEditor;
  private IStatus status;

  protected FieldEditorWrapper() {
  }

  @Override
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
      throws CoreException {
    this.configElement = config;
  }

  @Override
  public Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    parent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    fieldEditor = createFieldEditor(container);
    // FIXME: should we intercept #setMessage() and #setErrorMessage()?
    // fieldEditor.setPage(getPage());
    fieldEditor.setPropertyChangeListener(new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (FieldEditor.IS_VALID.equals(event.getProperty())) {
          fireValueChanged(IS_VALID, event.getOldValue(), event.getNewValue());
        } else if (FieldEditor.VALUE.equals(event.getProperty())) {
          fireValueChanged(VALUE, event.getOldValue(), event.getNewValue());
        }
      }
    });
    fieldEditor.setPreferenceStore(getPreferenceStore());
    fieldEditor.load();
    fieldEditor.fillIntoGrid(container, fieldEditor.getNumberOfControls());
    return container;
  }

  protected abstract FE createFieldEditor(Composite container);

  @Override
  public void dispose() {
    if (fieldEditor != null) {
      fieldEditor.dispose();
    }
  }


  @Override
  public IStatus getStatus() {
    if (fieldEditor == null || fieldEditor.isValid()) {
      return Status.OK_STATUS;
    }
    return new Status(IStatus.ERROR, configElement.getNamespaceIdentifier(),
        "FIXME: field is invalid");
  }

  @Override
  public void performApply() {
    fieldEditor.store();
  }

  @Override
  public void load() {
    fieldEditor.load();
  }

  @Override
  public void loadDefault() {
    fieldEditor.loadDefault();
  }
}
