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

package com.google.cloud.tools.eclipse.ui.util.databinding;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.core.runtime.IStatus;
import org.junit.Test;

public class BucketNameValidatorTest {

  private static final String LENGTH_63 = "123456789012345678901234567890123456789012345678901234567890123";
  private static final String LENGTH_64_WITH_DOT = "12345678901234567890123456789012345678901234567890123456789012.4";
  private static final String LENGTH_222 = "12345678901234567890123456789012345678901234567890."
                                           + "12345678901234567890123456789012345678901234567890."
                                           + "12345678901234567890123456789012345678901234567890."
                                           + "12345678901234567890123456789012345678901234567890."
                                           + "123456789012345678";
  
  private BucketNameValidator validator = new BucketNameValidator();

  @Test
  public void testValidation_nonStringInput() {
    IStatus status = validator.validate(new Object());
    assertThat(status.getSeverity(), is(IStatus.ERROR));
    assertThat(status.getMessage(), is("Invalid bucket name"));
  }

  @Test
  public void testValidation_emptyString() {
    assertThat(validator.validate("").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidation_upperCaseLetter() {
    IStatus status = validator.validate("THISWOULDBEVALIDIFLOWERCASE");
    assertThat(status.getSeverity(), is(IStatus.ERROR));
    assertThat(status.getMessage(), is("Invalid bucket name: THISWOULDBEVALIDIFLOWERCASE"));
  }

  @Test
  public void testValidation_startWithDot() {
    assertThat(validator.validate(".bucket").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidation_endWithDot() {
    assertThat(validator.validate("bucket.").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidation_startWithHyphen() {
    assertThat(validator.validate("-bucket").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidation_endWithHyphen() {
    assertThat(validator.validate("bucket-").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidation_startWithUnderscore() {
    assertThat(validator.validate("_bucket").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidation_endWithUnderscore() {
    assertThat(validator.validate("bucket_").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidation_maxLengthWithoutDot() {
    assertThat(validator.validate(LENGTH_63).getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidation_tooLongNameWithoutDot() {
    assertThat(validator.validate(LENGTH_63 + "4").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidation_validNameWithDot() {
    assertThat(validator.validate(LENGTH_64_WITH_DOT).getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidation_tooLongNameWithDot() {
    assertThat(validator.validate(LENGTH_222 + "9").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidation_maxLengthWithDot() {
    assertThat(validator.validate(LENGTH_222).getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidation_emptyComponent() {
    assertThat(validator.validate("foo..bar").getSeverity(), is(IStatus.ERROR));
  }
}
