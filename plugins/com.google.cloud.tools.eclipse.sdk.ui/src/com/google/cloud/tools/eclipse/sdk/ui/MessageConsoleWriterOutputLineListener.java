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

package com.google.cloud.tools.eclipse.sdk.ui;

import org.eclipse.ui.console.MessageConsoleStream;

import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;

public class MessageConsoleWriterOutputLineListener implements ProcessOutputLineListener {
  private MessageConsoleStream stream;

  public MessageConsoleWriterOutputLineListener(MessageConsoleStream stream) {
    this.stream = stream;
  }

  @Override
  public void onOutputLine(String line) {
    if (!stream.isClosed()) {
      // there's still a small chance that the stream will be closed and the error will be logged by the ConsolePlugin
      stream.println(line);
    }
  }
}