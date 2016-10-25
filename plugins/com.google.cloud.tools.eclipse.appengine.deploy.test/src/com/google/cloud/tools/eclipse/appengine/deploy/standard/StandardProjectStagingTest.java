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

package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;

@RunWith(MockitoJUnitRunner.class)
public class StandardProjectStagingTest {

  @Mock private IPath warDirectory;
  @Mock private IPath stagingDirectory;
  @Mock private CloudSdk cloudSdk;
  @Mock private IProgressMonitor monitor;

  @Test(expected = OperationCanceledException.class)
  public void testStage_cancelled() {
    when(monitor.isCanceled()).thenReturn(true);
    new StandardProjectStaging().stage(warDirectory, stagingDirectory, cloudSdk, monitor);
  }

}
