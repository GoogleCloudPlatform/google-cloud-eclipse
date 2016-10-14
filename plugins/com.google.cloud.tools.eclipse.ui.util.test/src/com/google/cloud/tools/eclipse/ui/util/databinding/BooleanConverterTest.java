package com.google.cloud.tools.eclipse.ui.util.databinding;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BooleanConverterTest {

  @Test
  public void testNegate() {
    assertTrue((Boolean) BooleanConverter.negate().convert(Boolean.FALSE));
    assertFalse((Boolean) BooleanConverter.negate().convert(Boolean.TRUE));
  }

}
