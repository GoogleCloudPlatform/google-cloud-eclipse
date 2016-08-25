package com.google.cloud.tools.eclipse.ui.util.databinding;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.core.runtime.IStatus;
import org.junit.Test;

public class ProjectIdValidatorTest {

  @Test
  public void testValidate_nonStringInput() {
    assertThat(new ProjectIdValidator().validate(new Object()).getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_emptyStringDefaultValidationPolicy() {
    assertThat(new ProjectIdValidator().validate("").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_emptyStringInvalid() {
    assertThat(new ProjectIdValidator()
                 .validate("", ProjectIdValidator.ValidationPolicy.EMPTY_IS_INVALID).getSeverity(),
               is(IStatus.ERROR));
  }

  @Test
  public void testValidate_emptyStringValid() {
    assertThat(new ProjectIdValidator()
                 .validate("", ProjectIdValidator.ValidationPolicy.EMPTY_IS_VALID).getSeverity(),
               is(IStatus.OK));
  }

  @Test
  public void testValidate_upperCaseLetter() {
    assertThat(new ProjectIdValidator().validate("asdfghijK").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_startWithNumber() {
    assertThat(new ProjectIdValidator().validate("1asdfghij").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_startWithHyphen() {
    assertThat(new ProjectIdValidator().validate("-asdfghij").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_endWithHyphen() {
    assertThat(new ProjectIdValidator().validate("asdfghij-").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_validName() {
    assertThat(new ProjectIdValidator().validate("asdf-1ghij-2").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_maxLengthName() {
    assertThat(new ProjectIdValidator().validate("a23456789012345678901234567890").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_tooLongName() {
    assertThat(new ProjectIdValidator().validate("a234567890123456789012345678901").getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_tooShortName() {
    assertThat(new ProjectIdValidator().validate("a2345").getSeverity(), is(IStatus.ERROR));
  }
}
