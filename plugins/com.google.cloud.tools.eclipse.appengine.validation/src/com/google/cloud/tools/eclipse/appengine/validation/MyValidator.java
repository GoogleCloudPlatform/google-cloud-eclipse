/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.validation;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.resources.IResource;
import org.eclipse.wst.validation.ValidationEvent;
import org.eclipse.wst.validation.ValidatorMessage;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;


public class MyValidator {
  
  public ValidationResult validate(ValidationEvent argEvent, ValidationState argState,
                                   IProgressMonitor argMonitor) {
    final IResource resource = argEvent.getResource();
    final ValidationResult result = new ValidationResult();
    try {
      if (projectId >= 0) {
        ValidatorMessage vm = ValidatorMessage.create("Message", resource);
        vm.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
        vm.setAttribute(IMarker.SOURCE_ID, IMarker.PROBLEM);
        vm.setAttribute(IMarker.LINE_NUMBER, 1);
        vm.setAttribute(IMarker.CHAR_START, 0);
        vm.setAttribute(IMarker.CHAR_END, 5);
        result.add(vm);
      }
     
    } catch (Exception ex) {
      ValidationPlugin.getDefault().warn(ex);
      result.add(ValidatorMessage.create(ex.toString(), resource));
    }
    return result;
  }
}
