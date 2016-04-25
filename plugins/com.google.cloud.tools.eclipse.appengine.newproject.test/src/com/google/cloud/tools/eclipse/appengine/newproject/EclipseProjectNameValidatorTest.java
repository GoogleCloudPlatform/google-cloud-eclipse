package com.google.cloud.tools.eclipse.appengine.newproject;

import org.junit.Assert;
import org.junit.Test;

public class EclipseProjectNameValidatorTest {

  @Test
  public void testAcceptableName() {
    Assert.assertTrue(EclipseProjectNameValidator.validate("A Cool Project"));
  }
  
  @Test
  public void testSlash() {
    Assert.assertFalse(EclipseProjectNameValidator.validate("cool/project"));
  }
  
  @Test
  public void testOnlySpaces() {
    Assert.assertFalse(EclipseProjectNameValidator.validate("    "));
  }
  
  @Test
  public void testEmptyString() {
    Assert.assertFalse(EclipseProjectNameValidator.validate(""));
  }
  
  @Test
  public void testNull() {
    Assert.assertFalse(EclipseProjectNameValidator.validate(null));
  }

}
