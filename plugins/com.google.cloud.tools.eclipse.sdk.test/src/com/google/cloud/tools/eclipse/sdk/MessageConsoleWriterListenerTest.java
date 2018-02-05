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

import static org.mockito.Mockito.verify;

import org.eclipse.ui.console.MessageConsoleStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessageConsoleWriterListenerTest {
  @Mock private MessageConsoleStream mockConsoleStream;

  @Test
  public void testOnOutputLine() {
    MessageConsoleWriterListener listener = new MessageConsoleWriterListener(mockConsoleStream);
    listener.onOutputLine("a message");
    verify(mockConsoleStream).println("a message");
  }

  @Test
  public void testMessage() {
    MessageConsoleWriterListener listener = new MessageConsoleWriterListener(mockConsoleStream);
    listener.message("a message");
    verify(mockConsoleStream).print("a message");
  }
}
