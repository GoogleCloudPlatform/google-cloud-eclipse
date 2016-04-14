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
  
  @Test
  public void testWhitelisted_nonJreClass() {
    Assert.assertTrue(AppEngineJreWhitelist.contains("com.google.Bar"));
  }
  
  @Test
  public void testWhitelisted_OmgClass() {
    Assert.assertFalse(AppEngineJreWhitelist.contains("org.omg.CosNaming.BindingIterator"));
  }
  
  @Test
  public void testWhitelisted_GssClass() {
    Assert.assertFalse(AppEngineJreWhitelist.contains("org.ietf.jgss.GSSContext"));
  }
  
  @Test
  public void testWhitelisted_JavaxClass() {
    Assert.assertTrue(AppEngineJreWhitelist.contains("javax.servlet.ServletRequest"));
  }
  
  @Test
  public void testWhitelisted_SwingClass() {
    Assert.assertFalse(AppEngineJreWhitelist.contains("javax.swing.JFrame"));
  }
  
}

