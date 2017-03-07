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

package com.google.cloud.tools.eclipse.ui.util.databinding;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Suppliers;
import org.eclipse.core.runtime.IStatus;
import org.junit.Test;

public class ProjectSelectorValidatorTest {

  @Test
  public void testValidate_nullString() {
    assertEquals(IStatus.ERROR, new ProjectSelectorValidator(Suppliers.ofInstance(Boolean.TRUE))
        .validate(null).getSeverity());
  }

  @Test
  public void testValidate_emptyString() {
    assertEquals(IStatus.ERROR, new ProjectSelectorValidator(Suppliers.ofInstance(Boolean.TRUE))
        .validate("").getSeverity());
  }

  @Test
  public void testValidate_noProjectsFound() {
    assertEquals(IStatus.ERROR, new ProjectSelectorValidator(Suppliers.ofInstance(Boolean.FALSE))
        .validate("").getSeverity());
  }

  @Test
  public void testValidate_valid() {
    assertEquals(IStatus.OK, new ProjectSelectorValidator(Suppliers.ofInstance(Boolean.TRUE))
        .validate("foo").getSeverity());
  }
}
