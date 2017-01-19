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

package com.google.cloud.tools.eclipse.sdk;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Predicate;
import java.util.List;
import org.junit.Test;

public class CollectingLineListenerTest {

  @Test(expected = NullPointerException.class)
  public void testGetCollectedMessage_nullPredicateGivesError() {
      new CollectingLineListener(null);
  }

  @Test
  public void testGetCollectedMessage_whenPredicateIsFalseNoMessageIsCollected() {
    Predicate<String> predicate = mock(Predicate.class);
    when(predicate.apply(anyString())).thenReturn(false);
    CollectingLineListener listener = new CollectingLineListener(predicate);
    listener.onOutputLine("Error message");
    assertTrue(listener.getCollectedMessages().isEmpty());
  }

  @Test
  public void testGetCollectedMessage_whenPredicateIsTrueMessagesArecollected() {
    Predicate<String> predicate = mock(Predicate.class);
    when(predicate.apply(anyString())).thenReturn(true);
    CollectingLineListener listener = new CollectingLineListener(predicate);
    listener.onOutputLine("Error message1");
    listener.onOutputLine("Error message2");
    List<String> collectedMessages = listener.getCollectedMessages();
    assertThat(collectedMessages.size(), is(2));
    assertThat(collectedMessages.get(0), is("Error message1"));
    assertThat(collectedMessages.get(1), is("Error message2"));
  }
}
