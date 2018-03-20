/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.test.util;

import java.util.Objects;
import java.util.function.Function;
import org.junit.Assert;

/**
 * Assertion tests for Arrays
 */
public class ArrayAssertions {
  public static <T> void assertIsEmpty(T[] arr) {
    assertIsEmpty(null, arr, Objects::toString, 120);
  }

  public static <T> void assertIsEmpty(String message, T[] arr) {
    assertIsEmpty(message, arr, Objects::toString, 120);
  }

  public static <T> void assertIsEmpty(T[] arr, Function<T, String> printer) {
    assertIsEmpty(null, arr, printer, 120);
  }

  /**
   * Assert that {@code arr} is empty; if not, fail with a message showing as many elements will fit
   * until the total message size exceeds {@code maxLength}.
   */
  public static <T> void assertIsEmpty(String message, T[] arr, Function<T, String> printer,
      int maxLength) {
    if (arr == null) {
      Assert.fail("array is null");
    } else if (arr.length != 0) {
      StringBuilder sb = new StringBuilder();
      if (message != null) {
        sb.append(message).append(": ");
      }
      sb.append('[').append(printer.apply(arr[0]));
      for (int i = 1; i < arr.length && sb.length() < maxLength; i++) {
        sb.append(',');
        sb.append(printer.apply(arr[i]));
      }
      sb.append(']');
      Assert.fail(sb.toString());
    }
  }
}
