/*
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
 */

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.cloud.tools.eclipse.appengine.localserver.PreferencesInitializer;
import com.google.cloud.tools.eclipse.preferences.areas.FieldEditorWrapper;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.swt.widgets.Composite;

public class LaunchBehaviorsArea extends FieldEditorWrapper<BooleanFieldEditor> {
  @Override
  protected BooleanFieldEditor createFieldEditor(Composite container) {
    return new BooleanFieldEditor(PreferencesInitializer.LAUNCH_BROWSER,
        "Open start page on launch", container);
  }
}
