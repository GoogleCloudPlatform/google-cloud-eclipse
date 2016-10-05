package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FilterTest {

  @Test(expected = NullPointerException.class)
  public void testNullArgument_exclusionFilter() {
    Filter.exclusionFilter(null);
  }

  @Test(expected = NullPointerException.class)
  public void testNullArgument_inclusionFilter() {
    Filter.inclusionFilter(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyArgument_exclusionFilter() {
    Filter.exclusionFilter("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyArgument_inclusionFilter() {
    Filter.inclusionFilter("");
  }

  @Test
  public void testNonEmptyArgument_exclusionFilter() {
    Filter filter = Filter.exclusionFilter("a.b.c");
    assertThat(filter.getPattern(), is("a.b.c"));
    assertTrue(filter.isExclude());
  }
  
  @Test
  public void testNonEmptyConstructorArgument_inclusionFilter() {
    Filter filter = Filter.inclusionFilter("a.b.c");
    assertThat(filter.getPattern(), is("a.b.c"));
    assertFalse(filter.isExclude());
  }
}
