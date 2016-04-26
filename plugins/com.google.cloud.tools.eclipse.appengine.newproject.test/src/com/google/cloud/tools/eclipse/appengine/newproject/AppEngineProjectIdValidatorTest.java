package com.google.cloud.tools.eclipse.appengine.newproject;

import org.junit.Assert;
import org.junit.Test;

public class AppEngineProjectIdValidatorTest {
  
  @Test
  public void testDomain() {
    Assert.assertTrue(AppEngineProjectIdValidator.validate("google.com:mystore"));
  }

  @Test
  public void testOneWord() {
    Assert.assertTrue(AppEngineProjectIdValidator.validate("word"));
  }

  @Test
  public void testUpperCase() {
    Assert.assertFalse(AppEngineProjectIdValidator.validate("WORD"));
  }
  
  @Test
  public void testLongWord() {
    boolean validate = AppEngineProjectIdValidator.validate(
        "012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
    Assert.assertFalse(validate);
  }
  
  @Test
  public void testContainsSpace() {
    Assert.assertFalse(AppEngineProjectIdValidator.validate("com google eclipse"));
  }

  @Test
  public void testEmptyString() {
    Assert.assertTrue(AppEngineProjectIdValidator.validate(""));
  }
  
  @Test
  public void testNull() {
    Assert.assertFalse(AppEngineProjectIdValidator.validate(null));
  }

}
