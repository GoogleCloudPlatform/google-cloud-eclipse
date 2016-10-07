/*******************************************************************************
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
 *******************************************************************************/
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
