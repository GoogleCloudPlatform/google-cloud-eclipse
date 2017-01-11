/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.sdk.OutputCollectorOutputLineListener;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConsoleOutputBasedDeployErrorMessageProviderTest {

  private static final String ERROR_MESSAGE = "message";
  @Mock private OutputCollectorOutputLineListener listener;

  @Test(expected = NullPointerException.class)
  public void testConstructor_nullArgument() {
    new ConsoleOutputBasedDeployErrorMessageProvider(null);
  }

  @Test
  public void testErrorMessage_emptyCollectedMessageReturnsNull() {
    when(listener.getCollectedMessages()).thenReturn(Collections.<String>emptyList());
    ConsoleOutputBasedDeployErrorMessageProvider messageProvider =
        new ConsoleOutputBasedDeployErrorMessageProvider(listener);
    assertNull(messageProvider.getErrorMessage());
  }

  @Test
  public void testErrorMessage_singleMessageReturnsOneLine() {
    when(listener.getCollectedMessages()).thenReturn(Collections.singletonList(ERROR_MESSAGE));
    ConsoleOutputBasedDeployErrorMessageProvider messageProvider =
        new ConsoleOutputBasedDeployErrorMessageProvider(listener);
    assertThat(messageProvider.getErrorMessage(), is(ERROR_MESSAGE));
  }

  @Test
  public void testErrorMessage_twoMessagesAreSeparatedByNewLine() {
    when(listener.getCollectedMessages()).thenReturn(Arrays.asList(ERROR_MESSAGE, ERROR_MESSAGE));
    ConsoleOutputBasedDeployErrorMessageProvider messageProvider =
        new ConsoleOutputBasedDeployErrorMessageProvider(listener);
    assertThat(messageProvider.getErrorMessage(), is(ERROR_MESSAGE + '\n' + ERROR_MESSAGE));
  }
}
