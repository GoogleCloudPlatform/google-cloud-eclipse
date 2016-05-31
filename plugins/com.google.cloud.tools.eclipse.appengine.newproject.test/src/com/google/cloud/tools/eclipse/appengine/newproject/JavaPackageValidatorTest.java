package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.runtime.IStatus;
import org.junit.Assert;
import org.junit.Test;

public class JavaPackageValidatorTest {

  @Test
  public void testUsualPackage() {
    Assert.assertTrue(JavaPackageValidator.validate("com.google.eclipse").isOK());
  }
  
  @Test
  public void testEndsWithPeriod() {
    IStatus status = JavaPackageValidator.validate("com.google.eclipse.");
    Assert.assertFalse(status.isOK());
    Assert.assertEquals("com.google.eclipse. ends with a period.", status.getMessage());
  }
  
  @Test
  public void testStartsWithPeriod() {
    IStatus status = JavaPackageValidator.validate(".com.google.eclipse");
    Assert.assertFalse(status.isOK());
    Assert.assertEquals("A package name cannot start or end with a dot", status.getMessage());
  }

  @Test
  public void testOneWord() {
    Assert.assertTrue(JavaPackageValidator.validate("word").isOK());
  }
  
  @Test
  public void testContainsSpaceAroundPeriod() {
    Assert.assertFalse(JavaPackageValidator.validate("com. google.eclipse").isOK());
  }
  
  @Test
  public void testContainsInternalSpace() {
    Assert.assertFalse(JavaPackageValidator.validate("com.goo gle.eclipse").isOK());
  }
  
  @Test
  public void testDoublePeriod() {
    Assert.assertFalse(JavaPackageValidator.validate("com..google").isOK());
  }

  @Test
  public void testJavaKeyword() {
    Assert.assertFalse(JavaPackageValidator.validate("com.new").isOK());
  }
  
  @Test
  public void testEmptyString() {
    Assert.assertTrue(JavaPackageValidator.validate("").isOK());
  }
  
  @Test
  public void testNull() {
    Assert.assertFalse(JavaPackageValidator.validate(null).isOK());
    Assert.assertFalse(JavaPackageValidator.validate(null).isMultiStatus());
  }
  
}
