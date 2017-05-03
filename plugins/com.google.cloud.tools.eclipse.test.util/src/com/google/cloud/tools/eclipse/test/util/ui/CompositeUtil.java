/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.test.util.ui;

import com.google.common.base.Predicate;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class CompositeUtil {

  @SuppressWarnings("unchecked")
  public static <T> T findControl(Composite composite, final Class<T> type) {
    return (T) findControl(composite, new Predicate<Control>() {
      @Override
      public boolean apply(Control control) {
        return type.isInstance(control);
      }
    });
  }

  public static Control findControl(Composite composite, Predicate<Control> predicate) {
    for (Control control : composite.getChildren()) {
      if (predicate.apply(control)) {
        return control;
      } else if (control instanceof Composite) {
        Control result = findControl((Composite) control, predicate);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }
}
