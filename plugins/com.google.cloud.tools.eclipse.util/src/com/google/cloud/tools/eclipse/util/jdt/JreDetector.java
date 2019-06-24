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

package com.google.cloud.tools.eclipse.util.jdt;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

/**
 * Applies a set of heuristics to guess whether a {@link IVMInstall JDT VM installation} represents
 * a JRE or a JDK (i.e., whether there is a compiler available). The standard way to obtain the
 * compiler is via <code>the ToolProvider.getSystemJavaCompiler()</code>. On Java 7 and Java 8, this
 * call tries to resolve a class at runtime (<code>com.sun.tools.javac.api.JavacTool</code>) and
 * otherwise tries to resolve that class from a <code>lib/tools.jar</code> from the installation
 * root. On Java 9, the compiler is in the <code>java.compiler</code> module, which is represented
 * by a <code>jmods/java.compiler.jmod</code> file relative to the installation root.
 */
public class JreDetector {
  // Java Execution Environment IDs from the org.eclipse.jdt.launching.executionEnvironments
  // extension point
  private static final String JAVASE7 = "JavaSE-1.7";
  private static final String JAVASE8 = "JavaSE-1.8";
  private static final String JAVASE9 = "JavaSE-9";
  private static final String JAVASE10 = "JavaSE-10";

  /** Java Execution Environment IDs from most recent to oldest. */
  private static final String[] JAVASE_BY_RECENCY = {JAVASE10, JAVASE9, JAVASE8, JAVASE7};

  private static final Logger logger = Logger.getLogger(JreDetector.class.getName());

  /** Return {@code true} if the VM installation appears to be a JDK. */
  public static boolean isDevelopmentKit(IVMInstall install) {
    IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
    return isDevelopmentKit(manager, install);
  }

  @VisibleForTesting
  static boolean isDevelopmentKit(IExecutionEnvironmentsManager manager, IVMInstall install) {
    File root = install.getInstallLocation();

    String executionEnvironment = determineExecutionEnvironment(manager, install);
    switch (executionEnvironment) {
      case JAVASE7:
      case JAVASE8:
        // JDKs on Windows, Linux, and MacOS all have lib/tools.jar
        File tools = new File(root, "lib/tools.jar");
        return tools.exists();

      case JAVASE9:
      case JAVASE10:
        // JDKs on Windows, Linux, and MacOS all have jmods/java.compiler.mod
        File compilerModule = new File(root, "jmods/java.compiler.jmod");
        return compilerModule.exists();

      default:
        logger.log(Level.WARNING, "Unknown Java installation type: " + executionEnvironment);
        return false;
    }
  }

  /** Return the best Execution Environment ID for the given VM. */
  @VisibleForTesting
  static String determineExecutionEnvironment(
      IExecutionEnvironmentsManager manager, IVMInstall install) {
    // A VM may be marked for several Java execution environments (e.g., a Java 9 VM is compatible
    // with Java 8 and Java 7).  So check the EEs in order of most recent to oldest to return the
    // most specific possible.
    for (String environmentId : JAVASE_BY_RECENCY) {
      IExecutionEnvironment environment = manager.getEnvironment(environmentId);
      if (environment == null) {
        continue;
      }
      if (environment.getDefaultVM() == install) {
        return environment.getId();
      }
      for (IVMInstall vm : environment.getCompatibleVMs()) {
        if (vm == install) {
          return environmentId;
        }
      }
    }
    return "UNKNOWN";
  }
}
