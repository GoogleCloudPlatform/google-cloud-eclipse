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

import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ProcessOutputLineListener} which also collects output lines starting with a certain
 * prefix.
 * <p>
 * If the prefix is <code>null</code> or empty, it will not collect any lines.
 */
public class OutputCollectorOutputLineListener implements ProcessOutputLineListener {

  private final String prefix;
  private final List<String> collectedMessages = new ArrayList<>();
  private final ProcessOutputLineListener wrappedListener;

  /**
   * 
   * @param prefix collects all lines starting with this prefix. If <code>prefix</code> is
   * <code>null</code> or empty string, it will not collect any lines
   */
  public OutputCollectorOutputLineListener(ProcessOutputLineListener wrappedListener,
                                           String prefix) {
    this.wrappedListener = wrappedListener;
    this.prefix = prefix;
  }

  @Override
  public void onOutputLine(String line) {
    wrappedListener.onOutputLine(line);
    if (!Strings.isNullOrEmpty(prefix) && line.startsWith(prefix)) {
      collectedMessages.add(line);
    }
  }

  public List<String> getCollectedMessages() {
    return new ArrayList<>(collectedMessages);
  }
}
