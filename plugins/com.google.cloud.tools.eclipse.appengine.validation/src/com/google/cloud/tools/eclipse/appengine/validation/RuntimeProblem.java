/*
 * Copyright 2018 Google LLC
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

package com.google.cloud.tools.eclipse.appengine.validation;

import org.eclipse.core.resources.IMarker;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;

class RuntimeProblem extends ElementProblem {

  RuntimeProblem(String message, DocumentLocation start, int length) {
    super(message, "com.google.cloud.tools.eclipse.appengine.validation.runtimeMarker",
        IMarker.SEVERITY_WARNING,
        IMessage.NORMAL_SEVERITY,
        start, length, new UpgradeRuntimeQuickAssistProcessor());
  }

}
