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

package com.google.cloud.tools.eclipse.ui.util.console;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

/**
 * An {@link IPatternMatchListenerDelegate} implementation that converts the matched
 * section of the text in a {@link TextConsole} to a {@link BrowserSupportBasedHyperlink}. 
 */
public class PatternToHyperlinkConverter implements IPatternMatchListenerDelegate {

  static final Logger logger = Logger.getLogger(PatternToHyperlinkConverter.class.toString());

  @Override
  public void connect(TextConsole console) {
  }

  @Override
  public void disconnect() {
  }

  @Override
  public void matchFound(PatternMatchEvent event) {
    if (event.getSource() instanceof TextConsole) {
      try {
        final TextConsole console = (TextConsole) event.getSource();
        final int start = event.getOffset();
        final int length = event.getLength();
        IHyperlink link = new BrowserSupportBasedHyperlink(console.getDocument().get(start, length));
        console.addHyperlink(link, start, length);
      } catch (BadLocationException e) {
        logger.log(Level.SEVERE, "Cannot create hyperlink", e);
      }
    }
  }

}
