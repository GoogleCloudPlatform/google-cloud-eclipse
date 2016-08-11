package com.google.cloud.tools.eclipse.usagetracker;

import static org.mockito.Mockito.spy;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class OptInDialogTest {

  @Test
  public void testOpen_showDialogWhenNonOpenStatusFlag() {
    AtomicBoolean openStatus = spy(new AtomicBoolean(false));
    int returnCode = new OptInDialog(null, openStatus).open();
    Assert.assertEquals(OptInDialog.OK, returnCode);
  }

  @Test
  public void testOpen_skipDialogWhenOpenStatusFlag() {
    AtomicBoolean openStatus = new AtomicBoolean(true);
    int returnCode = new OptInDialog(null, openStatus).open();
    Assert.assertEquals(OptInDialog.CANCEL, returnCode);
  }

  @Test
  public void testClose_clearsOpenStatusFlag() {
    AtomicBoolean openStatus = new AtomicBoolean(true);
    new OptInDialog(null, openStatus).close();
    Assert.assertEquals(false, openStatus.get());
  }
}
