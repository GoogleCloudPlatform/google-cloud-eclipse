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

package com.google.cloud.tools.eclipse.util.status;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.util.io.DeleteAllVisitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.junit.Assert;
import org.junit.Test;

public class StatusUtilTest {

  @Test
  public void testError_withClass() {
    IStatus error = StatusUtil.error(StatusUtil.class, "test error msg");
    verifyStatus(error);
  }

  @Test
  public void testError_withClassAndException() {
    Throwable exception = new Exception();
    IStatus error = StatusUtil.error(StatusUtil.class, "test error msg", exception);
    verifyStatus(error);
    assertThat(error.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testError_withInstance() {
    IStatus error = StatusUtil.error(new DeleteAllVisitor(), "test error msg");
    verifyStatus(error);
  }

  @Test
  public void testError_withInstanceAndException() {
    Throwable exception = new Exception();
    IStatus error = StatusUtil.error(new DeleteAllVisitor(), "test error msg", exception);
    verifyStatus(error);
    assertThat(error.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testErrorMessage_Exception() {
    RuntimeException ex = mock(RuntimeException.class);
    when(ex.getMessage()).thenReturn("testing");
    IStatus status = StatusUtil.setErrorStatus(this, "test message from StatusUtilTest", ex);
    Assert.assertEquals("test message from StatusUtilTest: testing", status.getMessage());
  }

  @Test
  public void testErrorMessage_ExceptionWithoutMessage() {
    RuntimeException ex = mock(RuntimeException.class);
    IStatus status = StatusUtil.setErrorStatus(this, "test message from StatusUtilTest", ex);
    Assert.assertEquals("test message from StatusUtilTest", status.getMessage());
  }

  private void verifyStatus(IStatus error) {
    assertThat(error.getSeverity(), is(IStatus.ERROR));
    assertThat(error.getMessage(), is("test error msg"));
    assertThat(error.getPlugin(), is("com.google.cloud.tools.eclipse.util"));
  }

  @Test
  public void testMulti_noError() {
    IStatus error = StatusUtil.multi(StatusUtil.class, "test error msg");
    assertThat(error, instanceOf(MultiStatus.class));
    assertThat(error.getPlugin(), is("com.google.cloud.tools.eclipse.util"));
    assertThat(error.getMessage(), is("test error msg"));
  }

  @Test
  public void testMulti_withError() {
    Throwable exception = new RuntimeException();
    IStatus error = StatusUtil.multi(StatusUtil.class, "test error msg", exception);
    assertThat(error, instanceOf(MultiStatus.class));
    assertThat(error.getPlugin(), is("com.google.cloud.tools.eclipse.util"));
    assertThat(error.getMessage(), is("test error msg"));
    assertThat(error.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testFilter_normalStatus() {
    IStatus error = StatusUtil.error(StatusUtil.class, "test error msg");
    assertThat(StatusUtil.filter(error), is(sameInstance(error)));
  }

  @Test
  public void testFilter_multiStatus() {
    MultiStatus multi = StatusUtil.multi(StatusUtil.class, "test");
    multi.add(Status.OK_STATUS);
    multi.add(StatusUtil.error(StatusUtil.class, "test error msg"));
    multi.add(Status.OK_STATUS);

    IStatus result = StatusUtil.filter(multi);
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertThat(result, instanceOf(MultiStatus.class));
    assertEquals(1, ((MultiStatus) result).getChildren().length);
    verifyStatus(((MultiStatus) result).getChildren()[0]);
  }
}
