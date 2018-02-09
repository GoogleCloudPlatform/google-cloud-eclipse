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
  public void testCreate_errorWithClass() {
    IStatus error = StatusUtil.create(IStatus.ERROR, StatusUtil.class, "test error msg");
    verifyStatus(error, IStatus.ERROR);
  }

  @Test
  public void testCreate_errorWithClassAndException() {
    Throwable exception = new Exception();
    IStatus error = StatusUtil.create(IStatus.ERROR, StatusUtil.class, "test error msg", exception);
    verifyStatus(error, IStatus.ERROR);
    assertThat(error.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testCreate_warnWithClass() {
    IStatus warn = StatusUtil.create(IStatus.WARNING, StatusUtil.class, "test error msg");
    verifyStatus(warn, IStatus.WARNING);
  }

  @Test
  public void testCreate_warnWithClassAndException() {
    Throwable exception = new Exception();
    IStatus warn =
        StatusUtil.create(IStatus.WARNING, StatusUtil.class, "test error msg", exception);
    verifyStatus(warn, IStatus.WARNING);
    assertThat(warn.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testError_withClass() {
    IStatus error = StatusUtil.error(StatusUtil.class, "test error msg");
    verifyStatus(error, IStatus.ERROR);
  }
  
  @Test
  public void testError_withCode() {
    IStatus error = StatusUtil.error(StatusUtil.class, "test error msg", 356);
    verifyStatus(error, IStatus.ERROR);
    assertEquals(356, error.getCode());
  }

  @Test
  public void testError_nullSource() {
    IStatus error = StatusUtil.error(null, "test error msg");
    verifyStatus(error, IStatus.ERROR);
  }

  @Test
  public void testError_nonOsgiSource() {
    IStatus error = StatusUtil.error("a string from the system classloader", "test error msg");
    assertThat(error.getSeverity(), is(IStatus.ERROR));
    assertThat(error.getMessage(), is("test error msg"));
    assertThat(error.getPlugin(), is("java.lang.String"));
  }

  @Test
  public void testError_withClassAndException() {
    Throwable exception = new Exception();
    IStatus error = StatusUtil.error(StatusUtil.class, "test error msg", exception);
    verifyStatus(error, IStatus.ERROR);
    assertThat(error.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testError_withInstance() {
    IStatus error = StatusUtil.error(new DeleteAllVisitor(), "test error msg");
    verifyStatus(error, IStatus.ERROR);
  }

  @Test
  public void testError_withInstanceAndException() {
    Throwable exception = new Exception();
    IStatus error = StatusUtil.error(new DeleteAllVisitor(), "test error msg", exception);
    verifyStatus(error, IStatus.ERROR);
    assertThat(error.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testWarn_withClass() {
    IStatus error = StatusUtil.warn(StatusUtil.class, "test error msg");
    verifyStatus(error, IStatus.WARNING);
  }

  @Test
  public void testWarn_withClassAndException() {
    Throwable exception = new Exception();
    IStatus error = StatusUtil.warn(StatusUtil.class, "test error msg", exception);
    verifyStatus(error, IStatus.WARNING);
    assertThat(error.getException(), is(sameInstance(exception)));
  }

  @Test
  public void testWarn_withInstance() {
    IStatus error = StatusUtil.warn(new DeleteAllVisitor(), "test error msg");
    verifyStatus(error, IStatus.WARNING);
  }

  @Test
  public void testInfo_withClass() {
    IStatus error = StatusUtil.info(StatusUtil.class, "test error msg");
    verifyStatus(error, IStatus.INFO);
  }

  @Test
  public void testInfo_withInstance() {
    IStatus error = StatusUtil.info(new DeleteAllVisitor(), "test error msg");
    verifyStatus(error, IStatus.INFO);
  }

  private void verifyStatus(IStatus error, int severity) {
    assertThat(error.getSeverity(), is(severity));
    assertThat(error.getMessage(), is("test error msg"));
    assertThat(error.getPlugin(), is("com.google.cloud.tools.eclipse.util"));
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
    IStatus status = StatusUtil.setErrorStatus(this, "test message from StatusUtilTest", originalStatus);
    Assert.assertEquals("test message from StatusUtilTest: testing", status.getMessage());
  }

  @Test
  public void testMerge_nullStatus() {
    IStatus originalStatus = StatusUtil.info(this, "testing");
    IStatus status = StatusUtil.merge(null, originalStatus);
    Assert.assertSame(originalStatus, status);
  }
  
  @Test
  public void testErrorMessage_ExceptionWithoutMessage() {
    RuntimeException ex = mock(RuntimeException.class);
    IStatus status = StatusUtil.setErrorStatus(this, "test message from StatusUtilTest", ex);
    Assert.assertEquals("test message from StatusUtilTest", status.getMessage());
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
    verifyStatus(((MultiStatus) result).getChildren()[0], IStatus.ERROR);
  }

  @Test
  public void testFilter_okMultiStatus() {
    IStatus multi = StatusUtil.multi(StatusUtil.class, "test multi msg"); //$NON-NLS-1$
    assertThat(StatusUtil.filter(multi), is(Status.OK_STATUS));
  }


}
