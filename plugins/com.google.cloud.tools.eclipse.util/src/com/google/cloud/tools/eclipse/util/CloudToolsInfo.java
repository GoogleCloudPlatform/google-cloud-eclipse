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

import org.osgi.framework.FrameworkUtil;

/**
 * Provides generic information about the plug-in, such as a name to be used for usage
 * reporting and the current version, etc.
 */
public class CloudToolsInfo {

  // Don't change the value; this name is used as an originating "application" of usage metrics.
  public static String METRICS_NAME = "gcloud-eclipse-tools";

  public static String getToolsVersion() {
    return FrameworkUtil.getBundle(new CloudToolsInfo().getClass()).getVersion().toString();
  }
}
