/*
 * Copyright 2017 Google Inc.
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
 */

package com.google.cloud.tools.eclipse.util;

import com.google.cloud.tools.eclipse.util.FacetExistsPropertyTester;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link FacetExistsPropertyTester}
 */
public class FacetExistsPropertyTesterTest {
  private FacetExistsPropertyTester propertyTester = new FacetExistsPropertyTester();

  @Test
  public void test_facetExist() {
    Assert.assertTrue(propertyTester.test(null /* receiver */, "facetExists", null /* args */,
        "com.google.cloud.tools.eclipse.appengine.facets.standard"));
  }

  @Test
  public void test_facetDoesNotExist() {
    Assert.assertFalse(propertyTester.test(null /* receiver */, "facetExists", null /* args */,
        "fake.facet"));
  }
}
