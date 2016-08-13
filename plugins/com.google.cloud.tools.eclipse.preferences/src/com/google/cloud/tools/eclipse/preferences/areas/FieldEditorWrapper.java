/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Wrap an existing {@link FieldEditor}.
 *
 * @param <FET> the type of wrapped {@linkplain FieldEditor}
 */
public abstract class FieldEditorWrapper<FET extends FieldEditor> extends PreferenceArea {
  /**
   * A dummy page that exists for its #setMessage() and #setErrorMessage().
   */
  public class MessageGatheringDialogPage extends DialogPage {
    @Override
    public void createControl(Composite parent) {
      throw new UnsupportedOperationException();
    }
  }

  private FET fieldEditor;
  private MessageGatheringDialogPage messages = new MessageGatheringDialogPage();

  protected FieldEditorWrapper() {
  }

  @Override
  public Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    parent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    fieldEditor = createFieldEditor(container);
    fieldEditor.setPage(messages);
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

  protected abstract FET createFieldEditor(Composite container);

  @Override
  public void dispose() {
    if (fieldEditor != null) {
      fieldEditor.dispose();
    }
  }


  @Override
  public IStatus getStatus() {
    // DialogPage has an unnecessarily complex set of message possibilities
    int messageType = messages.getMessageType();
    String message = messages.getErrorMessage();
    if (message != null) {
      messageType = IMessageProvider.ERROR;
    } else {
      message = messages.getMessage();
    }
    int severity;
    switch (messageType) {
      case IMessageProvider.INFORMATION:
        severity = IStatus.INFO;
        break;
      case IMessageProvider.WARNING:
        severity = IStatus.WARNING;
        break;
      case IMessageProvider.ERROR:
        severity = IStatus.ERROR;
        break;
      default:
        return Status.OK_STATUS;
    }
    return new Status(severity, "com.google.cloud.tools.eclipse.preferences", message);
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
