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

package com.google.cloud.tools.eclipse.test.util.ui;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISources;

/**
 * Mocks up an {@link ExecutionEvent} object for use with Eclipse Commands/Handlers.
 */
public class ExecutionEventBuilder {

  private IEvaluationContext context;

  public ExecutionEventBuilder() {
    context = mock(IEvaluationContext.class);
  }

  public ExecutionEvent build() {
    return new ExecutionEvent(null /* command */, Collections.EMPTY_MAP, null /* trigger */,
        context);
  }

  public ExecutionEventBuilder withActiveShell(Shell shell) {
    when(context.getVariable(ISources.ACTIVE_SHELL_NAME)).thenReturn(shell);
    return this;
  }

  public ExecutionEventBuilder withCurrentSelection(Object... objects) {
    return withCurrentSelection(new StructuredSelection(objects));
  }

  public ExecutionEventBuilder withCurrentSelection(IStructuredSelection selection) {
    when(context.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME)).thenReturn(selection);
    return this;
  }
}
