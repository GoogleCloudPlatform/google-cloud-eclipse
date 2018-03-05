/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.sdk.internal;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Supplier;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

class ShowConsoleViewAction extends Action {

  private final IConsole console;
  private final Supplier<IConsoleManager> consoleManagerSupplier;

  ShowConsoleViewAction(IConsole console) {
    this(console, () -> ConsolePlugin.getDefault().getConsoleManager());
  }

  @VisibleForTesting
  ShowConsoleViewAction(IConsole console, Supplier<IConsoleManager> consoleManagerSupplier) {
    this.console = console;
    this.consoleManagerSupplier = consoleManagerSupplier;
  }

  @Override
  public void run() {
    consoleManagerSupplier.get().showConsoleView(console);
  }
}
