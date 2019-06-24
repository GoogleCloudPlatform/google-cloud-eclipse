/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.util.jobs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MutexRuleTest {

  private final MutexRule rule = new MutexRule("mutex used in a test");

  @Test
  public void testContains_self() {
    assertTrue(rule.contains(rule));
  }

  @Test
  public void testContains_others() {
    assertFalse(rule.contains(new MutexRule("mutex used in a test")));
  }

  @Test
  public void testIsConflicting_self() {
    assertTrue(rule.isConflicting(rule));
  }

  @Test
  public void testIsConflicting_others() {
    assertFalse(rule.isConflicting(new MutexRule("mutex used in a test")));
  }
}
