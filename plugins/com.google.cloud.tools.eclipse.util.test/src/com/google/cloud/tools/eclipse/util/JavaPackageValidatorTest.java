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

package com.google.cloud.tools.eclipse.util;

import org.eclipse.core.runtime.IStatus;
import org.junit.Assert;
import org.junit.Test;

public class JavaPackageValidatorTest {

  private static final String PLUGIN_ID = 
      "com.google.cloud.tools.eclipse.appengine.newproject.AppEngineStandard"; //$NON-NLS-1$
  
  @Test
  public void testUsualPackage() {
    Assert.assertTrue(JavaPackageValidator.validate("com.google.eclipse", PLUGIN_ID).isOK());
  }
  
  @Test
  public void testEndsWithPeriod() {
    IStatus status = JavaPackageValidator.validate("com.google.eclipse.", PLUGIN_ID);
    Assert.assertFalse(status.isOK());
    Assert.assertEquals("com.google.eclipse. ends with a period.", status.getMessage());
    Assert.assertEquals(PLUGIN_ID, status.getPlugin());
  }
  
  @Test
  public void testStartsWithPeriod() {
    IStatus status = JavaPackageValidator.validate(".com.google.eclipse", PLUGIN_ID);
    Assert.assertFalse(status.isOK());
    Assert.assertEquals("A package name cannot start or end with a dot", status.getMessage());
  }

  @Test
  public void testOneWord() {
    IStatus status = JavaPackageValidator.validate("word", PLUGIN_ID);
    Assert.assertTrue(status.isOK());
  }
  
  @Test
  public void testContainsSpaceAroundPeriod() {
    Assert.assertFalse(JavaPackageValidator.validate("com. google.eclipse", PLUGIN_ID).isOK());
  }
  
  @Test
  public void testContainsInternalSpace() {
    Assert.assertFalse(JavaPackageValidator.validate("com.goo gle.eclipse", PLUGIN_ID).isOK());
  }
  
  @Test
  public void testDoublePeriod() {
    Assert.assertFalse(JavaPackageValidator.validate("com..google", PLUGIN_ID).isOK());
  }

  @Test
  public void testJavaKeyword() {
    Assert.assertFalse(JavaPackageValidator.validate("com.new", PLUGIN_ID).isOK());
  }
  
  @Test
  public void testEmptyString() {
    Assert.assertTrue(JavaPackageValidator.validate("", PLUGIN_ID).isOK());
  }
  
  @Test
  public void testNull() {
    Assert.assertFalse(JavaPackageValidator.validate(null, PLUGIN_ID).isOK());
    Assert.assertFalse(JavaPackageValidator.validate(null, PLUGIN_ID).isMultiStatus());
  }
  
}
