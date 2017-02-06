package com.google.cloud.tools.eclipse.appengine.validation;

import org.junit.Assert;
import org.junit.Test;

public class MessagesTest {

  @Test
  public void test() {
    Assert.assertEquals(
        "application element not recommended",
        Messages.getString("application.element"));
  }

}
