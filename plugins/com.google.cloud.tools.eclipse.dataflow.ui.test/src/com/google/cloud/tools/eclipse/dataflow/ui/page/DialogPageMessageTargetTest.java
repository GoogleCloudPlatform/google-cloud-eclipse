/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.widgets.Composite;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link DialogPageMessageTarget}.
 */
@RunWith(JUnit4.class)
public class DialogPageMessageTargetTest {
  private DialogPage page;

  private DialogPageMessageTarget target;

  @Before
  public void setup() {
    page = new DialogPage() {
      @Override
      public void createControl(Composite parent) {
        throw new UnsupportedOperationException();
      }
    };
    target = new DialogPageMessageTarget(page);
  }

  @Test
  public void setInfo() {
    target.setInfo("info_msg");
    assertEquals("info_msg", page.getMessage());
    assertEquals(DialogPage.INFORMATION, page.getMessageType());
  }

  @Test
  public void setError() {
    target.setError("error_msg");
    assertEquals("error_msg", page.getMessage());
    assertEquals(DialogPage.ERROR, page.getMessageType());
  }

  @Test
  public void clear() {
    target.setError("error_msg");
    target.clear();
    assertNull(page.getMessage());
    assertEquals(DialogPage.NONE, page.getMessageType());
  }
}
