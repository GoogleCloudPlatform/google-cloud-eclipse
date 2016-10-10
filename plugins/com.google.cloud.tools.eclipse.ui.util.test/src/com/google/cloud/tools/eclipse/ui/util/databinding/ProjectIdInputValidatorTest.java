package com.google.cloud.tools.eclipse.ui.util.databinding;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.core.runtime.IStatus;
import org.junit.Test;

public class ProjectIdInputValidatorTest {

  @Test
  public void testValidate_nonStringInput() {
    assertThat(new ProjectIdInputValidator(true /* emptyStringAllowed*/).validate(new Object()).getSeverity(), is(IStatus.ERROR));
    assertThat(new ProjectIdInputValidator(false /* emptyStringAllowed*/).validate(new Object()).getSeverity(), is(IStatus.ERROR));
  }

  @Test
  public void testValidate_emptyString() {
    assertThat(new ProjectIdInputValidator(true /* emptyStringAllowed*/).validate("").getSeverity(),is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(false /* emptyStringAllowed*/).validate("").getSeverity(),is(IStatus.ERROR));
  }

  @Test
  public void testValidate_upperCaseLetter() {
    assertThat(new ProjectIdInputValidator(true /* emptyStringAllowed*/).validate("asdfghijK").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(false /* emptyStringAllowed*/).validate("asdfghijK").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_startWithNumber() {
    assertThat(new ProjectIdInputValidator(true /* emptyStringAllowed*/).validate("1asdfghij").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(false /* emptyStringAllowed*/).validate("1asdfghij").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_startWithHyphen() {
    assertThat(new ProjectIdInputValidator(true /* emptyStringAllowed*/).validate("-asdfghij").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(false /* emptyStringAllowed*/).validate("-asdfghij").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_endWithHyphen() {
    assertThat(new ProjectIdInputValidator(true /* emptyStringAllowed*/).validate("asdfghij-").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(false /* emptyStringAllowed*/).validate("asdfghij-").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_validName() {
    assertThat(new ProjectIdInputValidator(true /* emptyStringAllowed*/).validate("asdf-1ghij-2").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(false /* emptyStringAllowed*/).validate("asdf-1ghij-2").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_maxLengthName() {
    assertThat(new ProjectIdInputValidator(true /* emptyStringAllowed*/).validate("a23456789012345678901234567890").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(false /* emptyStringAllowed*/).validate("a23456789012345678901234567890").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_tooLongName() {
    assertThat(new ProjectIdInputValidator(true /* emptyStringAllowed*/).validate("a234567890123456789012345678901").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(false /* emptyStringAllowed*/).validate("a234567890123456789012345678901").getSeverity(), is(IStatus.OK));
  }

  @Test
  public void testValidate_tooShortName() {
    assertThat(new ProjectIdInputValidator(true /* emptyStringAllowed*/).validate("a2345").getSeverity(), is(IStatus.OK));
    assertThat(new ProjectIdInputValidator(false /* emptyStringAllowed*/).validate("a2345").getSeverity(), is(IStatus.OK));
  }
}
