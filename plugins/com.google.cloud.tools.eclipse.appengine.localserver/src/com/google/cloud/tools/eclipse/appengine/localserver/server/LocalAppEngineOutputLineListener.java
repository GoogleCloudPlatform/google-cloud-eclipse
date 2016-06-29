package com.google.cloud.tools.eclipse.appengine.localserver.server;

import org.eclipse.ui.console.MessageConsoleStream;

import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;

public class LocalAppEngineOutputLineListener implements ProcessOutputLineListener {
  private MessageConsoleStream stream;

  public LocalAppEngineOutputLineListener(MessageConsoleStream stream) {
    this.stream = stream;
  }

  @Override
  public void onOutputLine(String line) {
    stream.println(line);
  }
}
