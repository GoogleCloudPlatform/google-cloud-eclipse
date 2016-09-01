package com.google.cloud.tools.eclipse.ui.util.console;

import org.eclipse.ui.console.MessageConsole;

/**
 * A console that displays information for a run/debug session of the App Engine runtime
 */
public class TaggedMessageConsole<T> extends MessageConsole {
  private T tag;

  public TaggedMessageConsole(String name, T tag) {
    super(name, null);
    this.tag = tag;
  }

  public T getTag() {
    return tag;
  }
}
