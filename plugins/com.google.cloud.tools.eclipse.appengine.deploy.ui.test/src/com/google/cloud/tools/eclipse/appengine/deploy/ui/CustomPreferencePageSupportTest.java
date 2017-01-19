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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.junit.Test;

public class CustomPreferencePageSupportTest {

  @Test
  public void testHandleStatusChange_staleStatus() {
    assertFalse(CustomPreferencePageSupport.computeValid(true /* statusStale */, Status.OK_STATUS));
    assertFalse(CustomPreferencePageSupport.computeValid(true, StatusUtil.error(null, "")));
  }

  @Test
  public void testHandleStatusChange_nullStatus() {
    assertTrue(CustomPreferencePageSupport.computeValid(false, null));
  }

  @Test
  public void testHandleStatusChange_okStatus() {
    assertTrue(CustomPreferencePageSupport.computeValid(false, Status.OK_STATUS));
  }

  @Test
  public void testHandleStatusChange_infoStatus() {
    assertTrue(CustomPreferencePageSupport.computeValid(false, StatusUtil.info(null, "")));
  }

  @Test
  public void testHandleStatusChange_warnStatus() {
    assertTrue(CustomPreferencePageSupport.computeValid(false, StatusUtil.warn(null, "")));
  }

  @Test
  public void testHandleStatusChange_cancelStatus() {
    assertFalse(CustomPreferencePageSupport.computeValid(false, Status.CANCEL_STATUS));
  }

  @Test
  public void testHandleStatusChange_errorStatus() {
    assertFalse(CustomPreferencePageSupport.computeValid(false, StatusUtil.error(null, "")));
  }

  @Test
  public void testHandleStatusChange_validStatusButErrorCodeStatus() {
    IStatus status1 = new Status(Status.OK, "pluginId", Status.ERROR, "", null);
    assertFalse(CustomPreferencePageSupport.computeValid(false, status1));

    IStatus status2 = new Status(Status.INFO, "pluginId", Status.ERROR, "", null);
    assertFalse(CustomPreferencePageSupport.computeValid(false, status2));

    IStatus status3 = new Status(Status.WARNING, "pluginId", Status.ERROR, "", null);
    assertFalse(CustomPreferencePageSupport.computeValid(false, status3));
  }

  @Test
  public void testHandleStatusChange_validStatusAndNonErrorCodeStatus() {
    IStatus status1 = new Status(Status.OK, "pluginId", Status.OK, "", null);
    assertTrue(CustomPreferencePageSupport.computeValid(false, status1));

    IStatus status2 = new Status(Status.OK, "pluginId", Status.INFO, "", null);
    assertTrue(CustomPreferencePageSupport.computeValid(false, status2));

    IStatus status3 = new Status(Status.OK, "pluginId", Status.WARNING, "", null);
    assertTrue(CustomPreferencePageSupport.computeValid(false, status3));
  }
}
