/*******************************************************************************
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
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
