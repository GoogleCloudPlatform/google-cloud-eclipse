package com.google.cloud.tools.eclipse.ui.util.databinding;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BooleanConverterTest {

  @Test
  public void testNegate() {
    BooleanConverter converter = BooleanConverter.negate();
    assertTrue((boolean) converter.convert(Boolean.FALSE));
    assertFalse((boolean) converter.convert(Boolean.TRUE));
  }

}
