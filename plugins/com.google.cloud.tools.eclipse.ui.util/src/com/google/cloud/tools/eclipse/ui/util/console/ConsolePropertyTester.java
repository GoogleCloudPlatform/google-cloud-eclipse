package com.google.cloud.tools.eclipse.ui.util.console;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.console.IConsole;

/**
 * {@link PropertyTester} implementation that can be used to test various properties of {@link IConsole}s.
 * <p>
 * <i>Currently only the </i><code>name</code><i> property is supported for matching.</i>
 */
public class ConsolePropertyTester extends PropertyTester {

  private static final String NAME = "name";

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    IConsole console = (IConsole) receiver;
    if (NAME.equals(property)) {
      return (console.getName() == null && expectedValue == null)
          || console.getName().equals(expectedValue);
    }
    return false;
  }

}
