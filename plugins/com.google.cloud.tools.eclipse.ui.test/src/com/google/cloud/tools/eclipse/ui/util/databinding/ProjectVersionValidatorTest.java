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

public class ProjectVersionValidatorTest {

  private static final String LENGTH_63 = "123456789012345678901234567890123456789012345678901234567890123";

  @Test
  public void testValidate_nonStringInput() {
    assertThat(new ProjectVersionValidator().validate(new Object()).getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_emptyString() {
    assertThat(new ProjectVersionValidator().validate("").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_reservedPrefix() {
    assertThat(new ProjectVersionValidator().validate("ah-asdfgh").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_reservedWordDefault() {
    assertThat(new ProjectVersionValidator().validate("default").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_reservedWordLatest() {
    assertThat(new ProjectVersionValidator().validate("latest").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_startWithHyphen() {
    assertThat(new ProjectVersionValidator().validate("-asdfgh").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_endWithHyphen() {
    assertThat(new ProjectVersionValidator().validate("asdfgh-").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_minimalLengthName() {
    assertThat(new ProjectVersionValidator().validate("a").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_maxLengthName() {
    assertThat(new ProjectVersionValidator().validate(LENGTH_63).getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_tooLongName() {
    assertThat(new ProjectVersionValidator().validate(LENGTH_63 + "4").getSeverity(), is(IStatus.ERROR));
  }
}
