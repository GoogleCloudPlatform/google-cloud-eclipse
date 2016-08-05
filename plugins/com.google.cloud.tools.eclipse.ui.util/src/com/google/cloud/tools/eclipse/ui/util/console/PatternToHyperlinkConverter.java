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
