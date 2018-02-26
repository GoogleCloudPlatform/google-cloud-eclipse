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

package com.google.cloud.tools.eclipse.sdk.internal;

import com.google.cloud.tools.managedcloudsdk.ProgressListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Wraps an {@link IProgressMonitor}. Assume that {@code totalWork} fits within an int which should
 * be safe as it seems unlikely that {@code appengine-plugins-core} will download anything &gt; 2gb.
 */
public class ProgressWrapper implements ProgressListener {
  private final IProgressMonitor wrapped;

  private final SubMonitor progress;

  public ProgressWrapper(IProgressMonitor progressMonitor) {
    // Store wrapped monitor as SubMonitor#convert() says that caller is responsible for calling
    // done() on argument
    progress = SubMonitor.convert(progressMonitor);
    wrapped = progressMonitor;
  }

  @Override
  public void start(String message, long totalWork) {
    progress.beginTask(message, (int) totalWork);
  }

  @Override
  public void update(long workDone) {
    progress.worked((int) workDone);
  }

  @Override
  public void update(String message) {
    progress.subTask(message);
  }

  @Override
  public void done() {
    progress.done();
    wrapped.done(); // SubMonitor#done() does not call done() on wrapped monitor
  }

  @Override
  public ProgressListener newChild(long allocation) {
    return new ProgressWrapper(progress.split((int) allocation));
  }
}
