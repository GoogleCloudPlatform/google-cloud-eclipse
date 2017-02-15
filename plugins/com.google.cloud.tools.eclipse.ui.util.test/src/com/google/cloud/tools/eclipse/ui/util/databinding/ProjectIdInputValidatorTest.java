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

public class ProjectIdInputValidatorTest {

  @Test
  public void testValidate_nonStringInput() {
    assertThat(new ProjectIdInputValidator(false /* requireProjectId */).validate(new Object()).getSeverity(), is(IStatus.ERROR));
    assertThat(new ProjectIdInputValidator(true /* requireProjectId */).validate(new Object()).getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_emptyString() {
    assertThat(new ProjectIdInputValidator(false /* requireProjectId */).validate("").getSeverity(),is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(true /* requireProjectId */).validate("").getSeverity(),is(IStatus.ERROR));
  }

  @Test
  public void testValidate_upperCaseLetter() {
    assertThat(new ProjectIdInputValidator(false /* requireProjectId */).validate("asdfghijK").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(true /* requireProjectId */).validate("asdfghijK").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_startWithNumber() {
    assertThat(new ProjectIdInputValidator(false /* requireProjectId */).validate("1asdfghij").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(true /* requireProjectId */).validate("1asdfghij").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_startWithHyphen() {
    assertThat(new ProjectIdInputValidator(false /* requireProjectId */).validate("-asdfghij").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(true /* requireProjectId */).validate("-asdfghij").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_endWithHyphen() {
    assertThat(new ProjectIdInputValidator(false /* requireProjectId */).validate("asdfghij-").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(true /* requireProjectId */).validate("asdfghij-").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_validName() {
    assertThat(new ProjectIdInputValidator(false /* requireProjectId */).validate("asdf-1ghij-2").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(true /* requireProjectId */).validate("asdf-1ghij-2").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_maxLengthName() {
    assertThat(new ProjectIdInputValidator(false /* requireProjectId */).validate("a23456789012345678901234567890").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(true /* requireProjectId */).validate("a23456789012345678901234567890").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_tooLongName() {
    assertThat(new ProjectIdInputValidator(false /* requireProjectId */).validate("a234567890123456789012345678901").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(true /* requireProjectId */).validate("a234567890123456789012345678901").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_tooShortName() {
    assertThat(new ProjectIdInputValidator(false /* requireProjectId */).validate("a2345").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(true /* requireProjectId */).validate("a2345").getSeverity(), is(IStatus.OK));
  }
}
