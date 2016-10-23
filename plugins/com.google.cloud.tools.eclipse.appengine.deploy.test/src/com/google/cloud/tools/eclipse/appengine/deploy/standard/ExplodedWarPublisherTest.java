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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExplodedWarPublisherTest {

  @Mock IProgressMonitor monitor;
  
  @Test(expected = NullPointerException.class)
  public void testWriteProjectToStageDir_nullProject() throws CoreException {
    new ExplodedWarPublisher().publish(null, null, monitor);
  }

  @Test(expected = NullPointerException.class)
  public void testWriteProjectToStageDir_nullStagingDir() throws CoreException {
    new ExplodedWarPublisher().publish(mock(IProject.class), null, monitor);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testWriteProjectToStageDir_emptyStagingDir() throws CoreException {
    new ExplodedWarPublisher().publish(mock(IProject.class), new Path(""), monitor);
  }
  
  @Test(expected = OperationCanceledException.class)
  public void testWriteProjectToStageDir_cancelled() throws CoreException {
    when(monitor.isCanceled()).thenReturn(true);
    new ExplodedWarPublisher().publish(mock(IProject.class), new Path(""), monitor);
  }
}
