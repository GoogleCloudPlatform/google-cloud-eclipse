package com.google.cloud.tools.eclipse.appengine.whitelist;

import org.junit.Assert;
import org.junit.Test;

public class JreWhitelistCheckerTest {

  private JreWhitelistChecker checker = new JreWhitelistChecker();

  @Test
  public void test() {
    Assert.assertFalse(checker.isActive(null));
  }

}
