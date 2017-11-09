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

package com.google.cloud.tools.eclipse.util;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;

public class AdapterUtil {

  private AdapterUtil() {}
  
  public static <T> T adapt(Object o, Class<T> cls) {
    if (cls.isInstance(o)) {
      return cls.cast(o);
    }
    if (o instanceof IAdaptable) {
      Object instance = ((IAdaptable) o).getAdapter(cls);
      if (instance != null) {
        return cls.cast(instance);
      }
    }
    Object adapted = Platform.getAdapterManager().loadAdapter(o, cls.getName());
    if (adapted != null) {
      return cls.cast(adapted);
    }
    return null;
  }
}
