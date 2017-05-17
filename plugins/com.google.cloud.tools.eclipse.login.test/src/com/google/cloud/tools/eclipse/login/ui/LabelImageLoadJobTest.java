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

package com.google.cloud.tools.eclipse.login.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.http.TestHttpServer;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Label;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LabelImageLoadJobTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();
  @Rule public TestHttpServer server = new TestHttpServer(
      "sample.gif", LabelImageLoaderTest.someImageBytes);

  private LabelImageLoadJob loadJob;
  private Label label;
  private URL url;

  @Before
  public void setUp() throws MalformedURLException {
    label = new Label(shellResource.getShell(), SWT.NONE);
    url = new URL(server.getAddress() + "sample.gif");
  }

  @After
  public void tearDown() {
    assertEquals(Job.NONE, loadJob.getState());

    if (!label.isDisposed()) {
      Image image = label.getImage();
      label.dispose();
      if (image != null) {
        assertTrue("FIX BUG: DisposeListener didn't run?", image.isDisposed());
      }
    }
    assertTrue("FIX BUG: DisposeListener didn't run?", loadJob.image.isDisposed());

    LabelImageLoader.cache.clear();
  }

  @Test
  public void testRun_imageStoredInCache() throws InterruptedException {
    assertTrue(LabelImageLoader.cache.isEmpty());

    loadJob = new LabelImageLoadJob(url, label);
    runAndWaitJob();
    assertNotNull(LabelImageLoader.cache.get(url.toString()));
  }

  @Test
  public void testRun_imageLoaded() throws InterruptedException {
    loadJob = new LabelImageLoadJob(url, label);
    runAndWaitJob();
    assertNotNull(label.getImage());
  }

  @Test
  public void testRun_imageDisposedByDisposeListener() throws InterruptedException {
    loadJob = new LabelImageLoadJob(url, label);
    runAndWaitJob();
    Image image = label.getImage();
    label.dispose();
    assertTrue(image.isDisposed());
    assertTrue(loadJob.image.isDisposed());
  }

  @Test
  public void testRun_noErrorIfLabelIsAlreadyDisposed()
      throws MalformedURLException, InterruptedException {
    URL url = new URL(server.getAddress() + "sample.gif");
    loadJob = new LabelImageLoadJob(url, label);

    label.dispose();
    runAndWaitJob();
    assertTrue(loadJob.image.isDisposed());
  }

  private void runAndWaitJob() throws InterruptedException {
    loadJob.schedule();
    while (!loadJob.join(100, null)) {  // spin and dispatch UI events
      shellResource.getDisplay().readAndDispatch();
    }
  }
}
