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
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ProcessOutputLineListener} that collects output lines satisfying some {@link Predicate}.
 */
public class CollectingLineListener implements ProcessOutputLineListener {

  private final Predicate<String> condition;
  private final List<String> collectedMessages = new ArrayList<>();

  /**
   * @param predicate all lines satisfying this predicate will be collected
   */
  public CollectingLineListener(Predicate<String> predicate) {
    Preconditions.checkNotNull(predicate, "predicate is null");
    this.condition = predicate;
  }

  /**
   * Evaluates the predicate on <code>line</code> and collects it if it's <code>true</code>.
   */
  @Override
  public void onOutputLine(String line) {
    if (condition.apply(line)) {
      collectedMessages.add(line);
    }
  }

  public List<String> getCollectedMessages() {
    return new ArrayList<>(collectedMessages);
  }
}
