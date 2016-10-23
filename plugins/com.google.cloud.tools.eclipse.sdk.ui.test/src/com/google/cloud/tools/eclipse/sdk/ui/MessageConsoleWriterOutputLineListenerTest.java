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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.ui.console.MessageConsoleStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.cloud.tools.eclipse.sdk.ui.MessageConsoleWriterOutputLineListener;

@RunWith(MockitoJUnitRunner.class)
public class MessageConsoleWriterOutputLineListenerTest {
  @Mock private MessageConsoleStream mockConsoleStream;

  /**
   * Tests that {@link MessageConsoleWriterOutputLineListener#onOutputLine(String)} appends the specified
   * message to the console stream.
   */
  @Test
  public void testOnOutputLine() {
    String message = "a message";
    MessageConsoleWriterOutputLineListener listener = new MessageConsoleWriterOutputLineListener(mockConsoleStream);
    listener.onOutputLine(message);
    verify(mockConsoleStream, times(1)).println(eq(message));
  }

}
