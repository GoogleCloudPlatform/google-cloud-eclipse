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
    assertThat(new ProjectVersionValidator().validate("").getSeverity(), is(IStatus.ERROR));
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
