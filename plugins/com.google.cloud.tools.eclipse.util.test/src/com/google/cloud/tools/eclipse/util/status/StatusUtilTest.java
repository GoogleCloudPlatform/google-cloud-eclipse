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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.junit.Assert;
import org.junit.Test;

public class StatusUtilTest {

  @Test
  public void testCreate_errorWithClass() {
    IStatus status = StatusUtil.create(IStatus.ERROR, StatusUtil.class, "test error msg");
    verifyStatus(status, IStatus.ERROR, "test error msg");
  }

  @Test
  public void testCreate_errorWithClassAndException() {
    Throwable exception = new Exception();
    IStatus status = StatusUtil.create(IStatus.ERROR, StatusUtil.class, "test error msg", exception);
    verifyStatus(status, IStatus.ERROR, "test error msg");
    assertThat(status.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testCreate_warnWithClass() {
    IStatus status = StatusUtil.create(IStatus.WARNING, StatusUtil.class, "test warning msg");
    verifyStatus(status, IStatus.WARNING, "test warning msg");
  }

  @Test
  public void testCreate_warnWithClassAndException() {
    Throwable exception = new Exception();
    IStatus status =
        StatusUtil.create(IStatus.WARNING, StatusUtil.class, "test warning msg", exception);
    verifyStatus(status, IStatus.WARNING, "test warning msg");
    assertThat(status.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testError_withClass() {
    IStatus status = StatusUtil.error(StatusUtil.class, "test error msg");
    verifyStatus(status, IStatus.ERROR, "test error msg");
  }
  
  @Test
  public void testError_withCode() {
    IStatus status = StatusUtil.error(StatusUtil.class, "test error msg", 356);
    verifyStatus(status, IStatus.ERROR, "test error msg");
    assertEquals(356, status.getCode());
  }

  @Test
  public void testError_nullSource() {
    IStatus status = StatusUtil.error(null, "test error msg");
    verifyStatus(status, IStatus.ERROR, "test error msg");
  }

  @Test
  public void testError_nonOsgiSource() {
    IStatus status = StatusUtil.error("a string from the system classloader", "test error msg");
    assertThat(status.getSeverity(), is(IStatus.ERROR));
    assertThat(status.getMessage(), is("test error msg"));
    assertThat(status.getPlugin(), is("java.lang.String"));
  }

  @Test
  public void testError_withClassAndException() {
    Throwable exception = new Exception();
    IStatus status = StatusUtil.error(StatusUtil.class, "test error msg", exception);
    verifyStatus(status, IStatus.ERROR, "test error msg");
    assertThat(status.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testError_withInstance() {
    IStatus status = StatusUtil.error(this, "test error msg");
    verifyStatus(status, IStatus.ERROR, "test error msg");
  }

  @Test
  public void testError_withInstanceAndException() {
    Throwable exception = new Exception();
    IStatus status = StatusUtil.error(this, "test error msg", exception);
    verifyStatus(status, IStatus.ERROR, "test error msg");
    assertThat(status.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testWarn_withClass() {
    IStatus status = StatusUtil.warn(StatusUtil.class, "test warning msg");
    verifyStatus(status, IStatus.WARNING, "test warning msg");
  }

  @Test
  public void testWarn_withClassAndException() {
    Throwable exception = new Exception();
    IStatus status = StatusUtil.warn(StatusUtil.class, "test warning msg", exception);
    verifyStatus(status, IStatus.WARNING, "test warning msg");
    assertThat(status.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testWarn_withInstance() {
    IStatus status = StatusUtil.warn(this, "test warning msg");
    verifyStatus(status, IStatus.WARNING, "test warning msg");
  }

  @Test
  public void testInfo_withClass() {
    IStatus status = StatusUtil.info(StatusUtil.class, "test info msg");
    verifyStatus(status, IStatus.INFO, "test info msg");
  }

  @Test
  public void testInfo_withInstance() {
    IStatus status = StatusUtil.info(this, "test info msg");
    verifyStatus(status, IStatus.INFO, "test info msg");
  }

  private void verifyStatus(IStatus status, int severity, String message) {
    assertThat(status.getSeverity(), is(severity));
    assertThat(status.getMessage(), is(message));
    assertThat(status.getPlugin(), is("com.google.cloud.tools.eclipse.util"));
  }

  @Test
  public void testErrorMessage_Exception() {
    RuntimeException ex = mock(RuntimeException.class);
    when(ex.getMessage()).thenReturn("testing");
    IStatus status = StatusUtil.setErrorStatus(this, "test message from StatusUtilTest", ex);
    Assert.assertEquals("test message from StatusUtilTest: testing", status.getMessage());
  }

  @Test
  public void testErrorMessage_Status() {
    IStatus originalStatus = StatusUtil.info(this, "testing");
    IStatus status =
        StatusUtil.setErrorStatus(this, "test message from StatusUtilTest", originalStatus);
    Assert.assertEquals("test message from StatusUtilTest: testing", status.getMessage());
  }

  @Test
  public void testErrorMessage_ExceptionWithoutMessage() {
    RuntimeException ex = mock(RuntimeException.class);
    IStatus status = StatusUtil.setErrorStatus(this, "test message from StatusUtilTest", ex);
    Assert.assertEquals("test message from StatusUtilTest", status.getMessage());
  }

  @Test
  public void testErrorMessage_invocationTargetException() {
    RuntimeException cause = mock(RuntimeException.class);
    when(cause.getMessage()).thenReturn("from cause");
    InvocationTargetException ex = mock(InvocationTargetException.class);
    when(ex.getCause()).thenReturn(cause);
    IStatus status = StatusUtil.setErrorStatus(this, "test message from StatusUtilTest", ex);
    Assert.assertEquals("test message from StatusUtilTest: from cause", status.getMessage());
  }

  @Test
  public void testErrorMessage_invocationTargetExceptionWithNullCause() {
    InvocationTargetException ex = mock(InvocationTargetException.class);
    when(ex.getMessage()).thenReturn("no cause");
    IStatus status = StatusUtil.setErrorStatus(this, "test message from StatusUtilTest", ex);
    Assert.assertEquals("test message from StatusUtilTest: no cause", status.getMessage());
  }

  @Test
  public void testMerge_nullStatus() {
    IStatus originalStatus = StatusUtil.info(this, "testing");
    IStatus status = StatusUtil.merge(null, originalStatus);
    Assert.assertSame(originalStatus, status);
  }
  
  @Test
  public void testMulti_noError() {
    MultiStatus status = StatusUtil.multi(StatusUtil.class, "test OK msg");
    verifyStatus(status, IStatus.OK, "test OK msg");
  }

  @Test
  public void testMulti_withError() {
    Throwable exception = new RuntimeException();
    MultiStatus status = StatusUtil.multi(StatusUtil.class, "test OK msg", exception);
    verifyStatus(status, IStatus.OK, "test OK msg");
    assertThat(status.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testFilter_normalStatus() {
    IStatus status = StatusUtil.error(StatusUtil.class, "test error msg");
    assertThat(StatusUtil.filter(status), is(sameInstance(status)));
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
    verifyStatus(((MultiStatus) result).getChildren()[0], IStatus.ERROR, "test error msg");
  }

  @Test
  public void testFilter_okMultiStatus() {
    IStatus multi = StatusUtil.multi(StatusUtil.class, "test multi msg"); //$NON-NLS-1$
    assertThat(StatusUtil.filter(multi), is(Status.OK_STATUS));
  }

  @Test
  public void testMulti_emptyStatusList() {
    MultiStatus multi = StatusUtil.multi(StatusUtil.class, "test multi-status msg", new IStatus[0]);
    verifyStatus(multi, IStatus.OK, "test multi-status msg");
    assertEquals(0, multi.getChildren().length);
  }

  @Test
  public void testMulti_statusList() {
    IStatus someChild = StatusUtil.multi(this, "child OK status msg");
    IStatus[] statuses = {Status.OK_STATUS, Status.CANCEL_STATUS, someChild};
    MultiStatus multi = StatusUtil.multi(StatusUtil.class, "test multi-status msg", statuses);
    verifyStatus(multi, IStatus.CANCEL, "test multi-status msg");

    List<IStatus> children = Arrays.asList(multi.getChildren());
    assertEquals(3, children.size());
    assertThat(children, hasItem(Status.OK_STATUS));
    assertThat(children, hasItem(Status.CANCEL_STATUS));
    assertThat(children, hasItem(someChild));
  }

}
