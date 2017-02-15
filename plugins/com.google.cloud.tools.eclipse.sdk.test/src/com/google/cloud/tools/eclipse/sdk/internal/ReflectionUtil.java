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

package com.google.cloud.tools.eclipse.sdk.internal;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtil {

  private ReflectionUtil() {}

  /** Retrieve a field value. */
  public static <T> T getField(Object object, String fieldName, Class<T> fieldType)
      throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
      SecurityException {
    Field field = object.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return fieldType.cast(field.get(object));
  }

  /** Invoke a method. */
  public static <T> T invoke(Object object, String methodName, Class<T> returnType,
      Object... parameters)
      throws NoSuchMethodException, SecurityException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
    Class<?>[] parameterTypes = new Class<?>[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      parameterTypes[i] = parameters[i] == null ? Object.class : parameters[i].getClass();
    }
    Method method = object.getClass().getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    return returnType.cast(method.invoke(object, parameters));
  }
}
