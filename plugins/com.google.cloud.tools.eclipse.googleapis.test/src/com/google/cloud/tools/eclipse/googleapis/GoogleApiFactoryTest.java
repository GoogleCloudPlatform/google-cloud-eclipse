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

package com.google.cloud.tools.eclipse.googleapis;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.appengine.v1.Appengine.Apps;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import java.io.IOException;
import org.junit.Test;

public class GoogleApiFactoryTest {

  @Test
  public void testNewAppsApi_userAgentIsSet() throws IOException {
    Apps api = new GoogleApiFactory().newAppsApi(mock(Credential.class));
    assertThat(api.get("").getRequestHeaders().getUserAgent(),
               containsString(CloudToolsInfo.USER_AGENT));
  }

  @Test
  public void testNewProjectsApi_userAgentIsSet() throws IOException {
    Projects api = new GoogleApiFactory().newProjectsApi(mock(Credential.class));
    assertThat(api.get("").getRequestHeaders().getUserAgent(),
               containsString(CloudToolsInfo.USER_AGENT));
  }
}
