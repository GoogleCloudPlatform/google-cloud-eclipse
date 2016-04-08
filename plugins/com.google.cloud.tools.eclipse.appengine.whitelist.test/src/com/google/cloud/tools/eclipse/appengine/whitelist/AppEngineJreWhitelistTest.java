package com.google.cloud.tools.eclipse.appengine.whitelist;

import org.junit.Assert;
import org.junit.Test;

public class AppEngineJreWhitelistTest {

  @Test
  public void testNotWhitelisted() {
    Assert.assertFalse(AppEngineJreWhitelist.contains("java.net.CookieManager"));
  }
 
  @Test
  public void testWhitelisted() {
    Assert.assertTrue(AppEngineJreWhitelist.contains("java.lang.String"));
  }
}

