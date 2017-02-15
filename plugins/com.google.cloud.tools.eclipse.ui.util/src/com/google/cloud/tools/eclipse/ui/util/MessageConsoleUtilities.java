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

package com.google.cloud.tools.eclipse.ui.util;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

/**
 * Helper methods for dealing with {@link MessageConsole}s.
 */
public class MessageConsoleUtilities {

  /**
   * Returns a {@link MessageConsole} with the given
   * <code>consoleName</code>. If no console by that name exists then one is
   * created. The returned console is cleared; callers of this method can decide
   * when to activate it. The console will not be brought to the front.
   *
   * @param consoleName name of the console
   * @param imageDescriptor image descriptor to use
   * @return {@link MessageConsole} with the given <code>consoleName</code>
   */
  public static MessageConsole getMessageConsole(String consoleName,
      ImageDescriptor imageDescriptor) {
    return getMessageConsole(consoleName, imageDescriptor, false);
  }

  /**
   * Returns a {@link MessageConsole} with the given
   * <code>consoleName</code>. If no console by that name exists then one is
   * created. The returned console is cleared; callers of this method can decide
   * when to activate it.
   *
   * @param consoleName name of the console
   * @param imageDescriptor image descriptor to use
   * @param show if true the console will be brought to the front
   * @return {@link MessageConsole} with the given <code>consoleName</code>
   */
  public static MessageConsole getMessageConsole(String consoleName,
      ImageDescriptor imageDescriptor, boolean show) {
    MessageConsole messageConsole = null;
    IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
    for (IConsole console : consoleManager.getConsoles()) {
      if (console.getName().equals(consoleName)
          && console instanceof MessageConsole) {
        messageConsole = (MessageConsole) console;
        break;
      }
    }

    if (messageConsole == null) {
      messageConsole = new MessageConsole(consoleName, imageDescriptor);
      consoleManager.addConsoles(new IConsole[] {messageConsole});
    } else {
      messageConsole.clearConsole();
    }

    if (show) {
      consoleManager.showConsoleView(messageConsole);
    }
    return messageConsole;
  }
  
  public static <C extends MessageConsole> C findOrCreateConsole(String name, ConsoleFactory<C> factory) {
    ConsolePlugin plugin = ConsolePlugin.getDefault();
    IConsoleManager manager = plugin.getConsoleManager();
    IConsole[] consoles = manager.getConsoles();
    for (IConsole console : consoles) {
      if (name.equals(console.getName())) {
        @SuppressWarnings("unchecked")
        C result = (C) console;
        return result;
      }
    }
    // console not found, so create a new one
    C console = factory.createConsole(name);
    manager.addConsoles(new IConsole[]{console});
    return console;
  }

  public static <C extends MessageConsole> C createConsole(String name, ConsoleFactory<C> factory) {
    ConsolePlugin plugin = ConsolePlugin.getDefault();
    IConsoleManager manager = plugin.getConsoleManager();
    C console = factory.createConsole(name);
    manager.addConsoles(new IConsole[]{console});
    return console;
  }

  public interface ConsoleFactory<C extends MessageConsole> {
    C createConsole(String name);
  }
}
