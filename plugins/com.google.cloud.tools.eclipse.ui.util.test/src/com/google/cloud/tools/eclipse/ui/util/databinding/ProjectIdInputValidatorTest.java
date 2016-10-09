package com.google.cloud.tools.eclipse.ui.util.databinding;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.core.runtime.IStatus;
import org.junit.Test;

public class ProjectIdInputValidatorTest {

  @Test
  public void testValidate_nonStringInput() {
    assertThat(new ProjectIdInputValidator().validate(new Object()).getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_emptyStringDefaultValidationPolicy() {
    assertThat(new ProjectIdInputValidator().validate("").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_emptyStringInvalid() {
    assertThat(new ProjectIdInputValidator()
                 .validate("").getSeverity(),
               is(IStatus.ERROR));
  }

  @Test
  public void testValidate_upperCaseLetter() {
    assertThat(new ProjectIdInputValidator().validate("asdfghijK").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_startWithNumber() {
    assertThat(new ProjectIdInputValidator().validate("1asdfghij").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_startWithHyphen() {
    assertThat(new ProjectIdInputValidator().validate("-asdfghij").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_endWithHyphen() {
    assertThat(new ProjectIdInputValidator().validate("asdfghij-").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_validName() {
    assertThat(new ProjectIdInputValidator().validate("asdf-1ghij-2").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_maxLengthName() {
    assertThat(new ProjectIdInputValidator().validate("a23456789012345678901234567890").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_tooLongName() {
    assertThat(new ProjectIdInputValidator().validate("a234567890123456789012345678901").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_tooShortName() {
    assertThat(new ProjectIdInputValidator().validate("a2345").getSeverity(), is(IStatus.OK));
  }
}
