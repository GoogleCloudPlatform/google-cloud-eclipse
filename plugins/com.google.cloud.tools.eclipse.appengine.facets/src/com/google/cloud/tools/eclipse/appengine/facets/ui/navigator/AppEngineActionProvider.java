/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.facets.ui.navigator;

import com.google.common.base.Preconditions;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.OpenFileAction;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

/** Provides App Engine relevant actions. */
public class AppEngineActionProvider extends CommonActionProvider {
  private OpenFileAction openFileAction;

  @Override
  public void init(ICommonActionExtensionSite aSite) {
    super.init(aSite);
    openFileAction = new OpenFileAction(getWorkbenchPage());
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    menu.add(openFileAction);
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    if (openFileAction.isEnabled()) {
      actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openFileAction);
    }
  }

  /** Provides opportunity for actions to update based on current selection. */
  @Override
  public void setContext(ActionContext context) {
    if (context != null && context.getSelection() instanceof IStructuredSelection) {
      IStructuredSelection selection = (IStructuredSelection) context.getSelection();
      openFileAction.selectionChanged(selection);
    }
  }

  private IWorkbenchPage getWorkbenchPage() {
    IWorkbenchPage page = getActionSite().getViewSite().getAdapter(IWorkbenchPage.class);
    if (page != null) {
      return page;
    }
    IWorkbenchWindow window = getActionSite().getViewSite().getAdapter(IWorkbenchWindow.class);
    if (window != null) {
      return window.getActivePage();
    }
    IWorkbench workbench = getActionSite().getViewSite().getAdapter(IWorkbench.class);
    if (workbench == null) {
      workbench = PlatformUI.getWorkbench();
    }
    Preconditions.checkNotNull(workbench);
    window = workbench.getActiveWorkbenchWindow();
    if (window == null) {
      Preconditions.checkState(workbench.getWorkbenchWindowCount() > 0);
      window = workbench.getWorkbenchWindows()[0];
    }
    Preconditions.checkNotNull(window);
    return window.getActivePage();
  }
}
