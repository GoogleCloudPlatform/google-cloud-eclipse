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
package com.google.cloud.tools.eclipse.appengine.localserver.server;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.ui.console.MessageConsoleStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalAppEngineOutputLineListenerTest {
  @Mock private MessageConsoleStream mockConsoleStream;

  /**
   * Tests that {@link LocalAppEngineOutputLineListener#onOutputLine(String)} appends the specified
   * message to the console stream.
   */
  @Test
  public void testOnOutputLine() {
    String message = "a message";
    LocalAppEngineOutputLineListener listener =
        new LocalAppEngineOutputLineListener(mockConsoleStream);  
    listener.onOutputLine(message);
    verify(mockConsoleStream, times(1)).println(eq(message));
  }

}
