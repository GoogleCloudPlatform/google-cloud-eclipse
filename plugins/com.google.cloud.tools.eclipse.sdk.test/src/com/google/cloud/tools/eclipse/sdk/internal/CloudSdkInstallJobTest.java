/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.sdk.internal;

import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.jobs.Job;
import org.junit.Test;

public class CloudSdkInstallJobTest {

  @Test
  public void testRun() throws InterruptedException {
    Job job = new CloudSdkInstallJob(null);
    job.schedule();
    job.join();
    assertTrue(job.getResult().isOK());
  }
}
