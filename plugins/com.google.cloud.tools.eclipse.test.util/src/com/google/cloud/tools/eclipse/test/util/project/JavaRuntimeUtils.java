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

package com.google.cloud.tools.eclipse.test.util.project;

import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

/**
 * Utility methods for working with JDT Java Runtimes.
 */
public class JavaRuntimeUtils {
  /**
   * Return {@code true} if we have a Java 8 compatible VM available. Intended for use with
   * {@link org.junit.Assume}.
   */
  public static boolean hasJavaSE8() {
    IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
    IExecutionEnvironment java8 = manager.getEnvironment("JavaSE-1.8");
    return java8 != null && java8.getCompatibleVMs().length > 0;
  }


}
