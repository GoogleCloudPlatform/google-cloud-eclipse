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

package com.google.cloud.tools.eclipse.login.ui;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.login.ui.LoginServiceUi.LocalServerReceiverWrapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import org.junit.Assert;
import org.junit.Test;

public class LoginServiceUiTest {

  @Test
  public void testScheduleCodeWaitingJob_successfulLogin()
      throws IOException, InterruptedException {
    LocalServerReceiverWrapper codeReceiver = mock(LocalServerReceiverWrapper.class);
    when(codeReceiver.waitForCode()).thenReturn("some valid authorization code");

    Semaphore wait = new Semaphore(0);
    String[] codeHolder = new String[1];
    IOException[] exceptionHolder = new IOException[1];

    new LoginServiceUi(null, null, null)
        .scheduleCodeWaitingJob(codeReceiver, wait, codeHolder, exceptionHolder)
        .join();

    verify(codeReceiver).stop();
    Assert.assertEquals("some valid authorization code", codeHolder[0]);
    Assert.assertNull(exceptionHolder[0]);
    Assert.assertEquals(1, wait.availablePermits());
  }

  @Test
  public void testScheduleCodeWaitingJob_failedLogin()
      throws IOException, InterruptedException {
    LocalServerReceiverWrapper codeReceiver = mock(LocalServerReceiverWrapper.class);
    when(codeReceiver.waitForCode()).thenThrow(new IOException("some IOException"));

    Semaphore wait = new Semaphore(0);
    String[] codeHolder = new String[1];
    IOException[] exceptionHolder = new IOException[1];

    new LoginServiceUi(null, null, null)
        .scheduleCodeWaitingJob(codeReceiver, wait, codeHolder, exceptionHolder)
        .join();

    verify(codeReceiver).stop();
    Assert.assertNull(codeHolder[0]);
    Assert.assertEquals("some IOException", exceptionHolder[0].getMessage());
    Assert.assertEquals(1, wait.availablePermits());
  }

  private boolean cancelRequestReceived;

  @Test
  public void testStopCodeWaitingJob() throws IOException, InterruptedException {
    try (ServerSocket serverSocket = new ServerSocket()) {
      serverSocket.bind(null);
      Thread serverThread = createListenerThread(serverSocket);
      serverThread.start();

      new LoginServiceUi(null, null, null)
          .stopCodeWaitingJob("http://127.0.0.1:" + serverSocket.getLocalPort());

      serverThread.join(5000);  // Test should pass right away. Don't wait for too long.
      Assert.assertTrue(cancelRequestReceived);
    }
  }

  private Thread createListenerThread(final ServerSocket serverSocket) {
    return new Thread(new Runnable() {
      @Override
      public void run() {
        try (
          Socket socket = serverSocket.accept();
          InputStreamReader reader = new InputStreamReader(
              socket.getInputStream(), StandardCharsets.UTF_8);
          OutputStreamWriter writer = new OutputStreamWriter(
              socket.getOutputStream(), StandardCharsets.UTF_8);
        ) {
          StringBuilder input = new StringBuilder();
          for (int ch = reader.read(); ch != -1; ch = reader.read()) {
            input.append((char) ch);
            if (input.lastIndexOf("?error=canceled-by-user") != -1) {
              cancelRequestReceived = true;
              writer.write("HTTP/1.1 200 OK\n\n");
              break;
            }
          }
        } catch (IOException ioe) {}
      }
    });
  }
}
