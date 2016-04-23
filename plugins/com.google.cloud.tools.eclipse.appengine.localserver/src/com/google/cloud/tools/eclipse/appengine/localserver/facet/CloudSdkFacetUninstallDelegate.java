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
package com.google.cloud.tools.eclipse.appengine.localserver.facet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * Google Cloud SDK facet delegate for facet uninstall action.
 */
public class CloudSdkFacetUninstallDelegate implements IDelegate {
  /**
   * Facet removal is handled by
   * {@code FacetedProject#mergeChanges} and the additional work to manage the
   * installation of the gcloud/app engine component and the gcloud-maven plugin
   * is not yet supported.
   */
  @Override
  public void execute(IProject project,
                      IProjectFacetVersion facetVersion,
                      Object config,
                      IProgressMonitor monitor) throws CoreException {
    // Do nothing
  }
}
