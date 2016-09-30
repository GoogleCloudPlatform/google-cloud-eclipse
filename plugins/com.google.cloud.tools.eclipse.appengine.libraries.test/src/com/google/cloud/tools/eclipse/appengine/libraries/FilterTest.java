package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

public class FilterTest {

  @Test(expected = NullPointerException.class)
  public void testNullConstructorArgument() {
    new Filter(null) {
    };
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyConstructorArgument() {
    new Filter("") {
    };
  }

  @Test
  public void testNonEmptyConstructorArgument() {
    Filter filter = new Filter("a.b.c") {
    };
    assertThat(filter.getPattern(), is("a.b.c"));
  }
}
