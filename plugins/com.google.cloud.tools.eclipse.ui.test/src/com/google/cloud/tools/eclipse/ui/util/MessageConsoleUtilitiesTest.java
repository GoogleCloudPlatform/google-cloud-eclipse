/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.ui.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessageConsoleUtilitiesTest {

  @Mock private IConsoleManager consoleManager;
  @Mock private MessageConsole existingConsole;

  @Before
  public void setUp() {
    MessageConsoleUtilities.consoleManagerSupplier = () -> consoleManager;
    when(existingConsole.getName()).thenReturn("existing console");
    when(consoleManager.getConsoles()).thenReturn(new IConsole[] {existingConsole});
  }

  @Test
  public void testGetMessageConsole_newConsole() {
    IConsole created = MessageConsoleUtilities.getMessageConsole("new console", null);
    verify(consoleManager).addConsoles(new IConsole[] {created});
  }

  @Test
  public void testGetMessageConsole_existingConsole() {
    IConsole returned = MessageConsoleUtilities.getMessageConsole("existing console", null);
    assertEquals(returned, existingConsole);
    verify(consoleManager, never()).addConsoles(any(IConsole[].class));
  }

  @Test
  public void testGetMessageConsole_doNotClear() {
    MessageConsole returned = MessageConsoleUtilities.getMessageConsole("existing console", null);
    assertEquals(returned, existingConsole);
    verify(returned, never()).clearConsole();
  }

  @Test
  public void testGetMessageConsole_doNotShow() {
    MessageConsoleUtilities.getMessageConsole("new console", null);
    verify(consoleManager, never()).showConsoleView(any(IConsole.class));
  }

  @Test
  public void testGetMessageConsole_show() {
    MessageConsoleUtilities.getMessageConsole("new console", null, true);
    verify(consoleManager).showConsoleView(any(IConsole.class));
  }

  @Test
  public void testCreateConsole() {
    MessageConsole newConsole = mock(MessageConsole.class);
    IConsole created = MessageConsoleUtilities.createConsole("new console", unused -> newConsole);
    assertEquals(created, newConsole);
    verify(consoleManager).addConsoles(new IConsole[] {created});
  }

  @Test
  public void testFindOrCreateConsole_newConsole() {
    MessageConsole newConsole = mock(MessageConsole.class);
    IConsole created = MessageConsoleUtilities.findOrCreateConsole(
        "new console", unused -> newConsole);
    assertEquals(created, newConsole);
    verify(consoleManager).addConsoles(new IConsole[] {created});
  }

  @Test
  public void testCreateConsole_existingConsole() {
    IConsole returned = MessageConsoleUtilities.findOrCreateConsole(
        "existing console", unused -> null);
    assertEquals(returned, existingConsole);
    verify(consoleManager, never()).addConsoles(any(IConsole[].class));
  }
}
