/*******************************************************************************
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
 *******************************************************************************/
package com.google.cloud.tools.eclipse.ui.util.databinding;

import org.eclipse.core.databinding.conversion.Converter;

/**
 * Utility methods to provide custom Eclipse Databinding {@link Converter}s working with Boolean.
 */
public abstract class BooleanConverter extends Converter {

  /**
   * Returns a {@link Converter} that converts from Boolean to Boolean by negating the input value.
   */
  public static BooleanConverter negate() {
    return new BooleanConverter() {
      public Object convert(Object fromObject) {
        if (fromObject == null) {
          return Boolean.TRUE;
        } else {
          return !(Boolean) fromObject;
        }
      }
    };
  }

  protected BooleanConverter() {
    super(Boolean.class, Boolean.class);
  }
}
