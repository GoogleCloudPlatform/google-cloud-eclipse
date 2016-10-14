package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class LibraryRecommendationTest {

  @Test(expected = IllegalArgumentException.class)
  public void testValueOfWithLowerCase() {
    LibraryRecommendation.valueOf("optional");
  }

  public void testValueOf_OPTIONAL() {
    assertThat(LibraryRecommendation.valueOf("OPTIONAL"), is(LibraryRecommendation.OPTIONAL));
  }
}
