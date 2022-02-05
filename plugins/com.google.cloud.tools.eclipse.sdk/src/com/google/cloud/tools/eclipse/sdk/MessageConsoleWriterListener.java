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

package com.google.cloud.tools.eclipse.sdk;

import com.google.cloud.tools.appengine.operations.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.managedcloudsdk.ConsoleListener;
import org.eclipse.ui.console.MessageConsoleStream;

public class MessageConsoleWriterListener implements ProcessOutputLineListener, ConsoleListener {
  private final MessageConsoleStream stream;

  public MessageConsoleWriterListener(MessageConsoleStream stream) {
    this.stream = stream;
  }

  @Override
  public void onOutputLine(String line) {
    if (stream != null && !stream.isClosed()) {
      // there's still a small chance that the stream will be closed and the error will be logged by
      // the ConsolePlugin
      stream.println(line);
    }
  }

  @Override
  public void console(String rawString) {
    if (stream != null && !stream.isClosed()) {
      // there's still a small chance that the stream will be closed and the error will be logged by
      // the ConsolePlugin
      stream.print(rawString);
    }
  }
}