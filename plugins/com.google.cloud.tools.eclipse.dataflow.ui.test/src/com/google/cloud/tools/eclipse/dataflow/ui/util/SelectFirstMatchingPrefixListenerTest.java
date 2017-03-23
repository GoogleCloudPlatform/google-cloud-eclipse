/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.util;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.dataflow.ui.util.SelectFirstMatchingPrefixListener.OnCompleteListener;
import com.google.common.collect.ImmutableSortedSet;
import java.util.SortedSet;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link SelectFirstMatchingPrefixListener}.
 */
@RunWith(JUnit4.class)
public class SelectFirstMatchingPrefixListenerTest {
  @Mock
  private Combo combo;
  private SortedSet<String> elements =
      ImmutableSortedSet.of("collidingText1", "collidingText222", "exampleText");
  private SelectFirstMatchingPrefixListener listener;
  @Mock private OnCompleteListener onCompleteListener;
  
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    
    listener = new SelectFirstMatchingPrefixListener(combo);
    listener.setContents(elements);
    listener.addOnCompleteListener(onCompleteListener);
  }
    
  @Test
  public void testBelowPreviousNoAutofill() {
    when(combo.getText()).thenReturn("long_text_that_doesnt_match");
    // Updates the 
    listener.modifyText(mock(ModifyEvent.class));
    when(combo.getText()).thenReturn("exampleTex");
    listener.modifyText(mock(ModifyEvent.class));
    verify(combo, never()).select(anyInt());
    verify(combo, never()).setSelection(any(Point.class));
  }
  
  @Test
  public void testBelowThresholdNoAutofill() {
    when(combo.getText()).thenReturn("long_text_that_doesnt_match");
    // Updates the combo to be initialized with "long_text_that_doesnt_match" as the current value
    listener.modifyText(mock(ModifyEvent.class));
    when(combo.getText()).thenReturn("ex");
    listener.modifyText(mock(ModifyEvent.class));
    verify(combo, never()).select(anyInt());
    verify(combo, never()).setSelection(any(Point.class));  
  }
  
  @Test
  public void testNoPrefixMatchSucceeds() {
    when(combo.getText()).thenReturn("long_text_that_doesnt_match");
    // Updates the combo to be initialized with "long_text_that_doesnt_match" as the current value
    listener.modifyText(mock(ModifyEvent.class));
    when(combo.getText()).thenReturn("also_doesnt_match");
    listener.modifyText(mock(ModifyEvent.class));
    verify(combo, never()).select(anyInt());
    verify(combo, never()).setSelection(any(Point.class));  
  }
  
  @Test
  public void testPrefixMatchAutofills() {
    when(combo.getText()).thenReturn("long_text_that_doesnt_match");
    // Updates the combo to be initialized with "long_text_that_doesnt_match" as the current value
    listener.modifyText(mock(ModifyEvent.class));
    String prefix = "exampleTe";
    when(combo.getText()).thenReturn(prefix);
    listener.modifyText(mock(ModifyEvent.class));
    verify(combo).select(2);
    verify(combo).setSelection(new Point(prefix.length(), "exampleText".length()));  
  }
  
  @Test
  public void testPrefixMatchMultipleMatches() {
    when(combo.getText()).thenReturn("long_text_that_doesnt_match");
    // Updates the combo to be initialized with "long_text_that_doesnt_match" as the current value
    listener.modifyText(mock(ModifyEvent.class));
    String prefix = "collidingT";
    when(combo.getText()).thenReturn(prefix);
    listener.modifyText(mock(ModifyEvent.class));
    verify(combo).select(0); // == "collidingText1" - it's lexicographically the earliest collision
    verify(combo).setSelection(new Point(prefix.length(), "collidingText1".length()));  
  }
}
