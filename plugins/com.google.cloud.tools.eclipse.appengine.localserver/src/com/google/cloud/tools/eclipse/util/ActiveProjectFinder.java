/*******************************************************************************
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.cloud.tools.eclipse.util;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Helper class to retrieve the currently selected project.
 */
public class ActiveProjectFinder {
  /**
   * Retrieve the selected project from a handler
   * 
   * @param event the handler execution context
   * @return a project or {@code null}
   */
  public static IProject getSelectedProject(ExecutionEvent event) {
    return getSelectedProject(HandlerUtil.getCurrentSelection(event));
  }

  /**
   * Retrieve the selected project from a selection
   * 
   * @param event the handler execution context
   * @return a project or {@code null}
   */
  public static IProject getSelectedProject(ISelection selection) {
    IResource selectionResource = ResourceUtils.getSelectionResource(selection);
    if (selectionResource == null) {
      return null;
    }
    return selectionResource.getProject();
  }

  private ActiveProjectFinder() {
  }

}
