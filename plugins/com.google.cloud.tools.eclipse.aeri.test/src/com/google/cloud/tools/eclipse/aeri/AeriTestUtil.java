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

package com.google.cloud.tools.eclipse.aeri;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.epp.logging.aeri.core.IStackTraceElement;
import org.eclipse.epp.logging.aeri.core.IThrowable;

public class AeriTestUtil {

  static Throwable sampleThrowable;
  static {
    sampleThrowable = new IllegalCharsetNameException("no CT4E packages in stack trace");
    sampleThrowable.setStackTrace(new StackTraceElement[] {
        new StackTraceElement("com.example.Charlie", "boom", "Charlie.java", 34),
        new StackTraceElement("com.example.Bob", "callCharlie", "Bob.java", 30),
        new StackTraceElement("com.example.Alice", "callBob", "Alice.java", 26),
        new StackTraceElement("java.lang.Thread", "run", "Thread.java", 745)
    });
  }

  /** Converts standard {@code Throwable} into AERI's {@code IThrowable} as a mock. */
  static IThrowable getMockIThrowable(Throwable throwable) {
    IThrowable aeriThrowable = mock(IThrowable.class);
    when(aeriThrowable.getClassName()).thenReturn(throwable.getClass().getName());
    when(aeriThrowable.getMessage()).thenReturn(throwable.getMessage());

    FakeAeriEList aeriStackTrace = new FakeAeriEList();
    when(aeriThrowable.getStackTrace()).thenReturn(aeriStackTrace);

    for (StackTraceElement frame : throwable.getStackTrace()) {
      IStackTraceElement aeriFrame = mock(IStackTraceElement.class);
      aeriStackTrace.add(aeriFrame);

      when(aeriFrame.getClassName()).thenReturn(frame.getClassName());
      when(aeriFrame.getMethodName()).thenReturn(frame.getMethodName());
      when(aeriFrame.getFileName()).thenReturn(frame.getFileName());
      when(aeriFrame.getLineNumber()).thenReturn(frame.getLineNumber());
    }
    return aeriThrowable;
  }

  /** Quick fake (but almost working) implementation of AERI EList for testing. */
  @SuppressWarnings("serial")
  private static class FakeAeriEList extends ArrayList<IStackTraceElement>
      implements EList<IStackTraceElement> {
    @Override
    public void move(int newPosition, IStackTraceElement object) {}

    @Override
    public IStackTraceElement move(int newPosition, int oldPosition) {
      return null;
    }
  }
}
