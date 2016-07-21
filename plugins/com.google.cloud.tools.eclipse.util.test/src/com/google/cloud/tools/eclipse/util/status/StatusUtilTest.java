package com.google.cloud.tools.eclipse.util.status;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;

import org.eclipse.core.runtime.IStatus;
import org.junit.Test;

import com.google.cloud.tools.eclipse.util.io.DeleteAllVisitor;

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
  public void testError_withClassAsObject() {
    IStatus error = StatusUtil.error((Object) new DeleteAllVisitor().getClass(), "test error msg");
    verifyStatus(error);
  }

  @Test
  public void testError_withClassAsObjectAndException() {
    Throwable exception = new Exception();
    IStatus error = StatusUtil.error((Object) new DeleteAllVisitor().getClass(), "test error msg", exception);
    verifyStatus(error);
    assertThat(error.getException(), is(sameInstance(exception)));
  }

  private void verifyStatus(IStatus error) {
    assertThat(error.getSeverity(), is(IStatus.ERROR));
    assertThat(error.getMessage(), is("test error msg"));
    assertThat(error.getPlugin(), is("com.google.cloud.tools.eclipse.util"));
  }
}
