/*******************************************************************************
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.login.ui;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.widgets.Widget;
import org.junit.Test;

public class GoogleLoginBrowserTest {

  @Test
  public void testTitleListener_nonCodeTitle() {
    GoogleLoginBrowser loginBrowser = mock(GoogleLoginBrowser.class);
    TitleEvent titleEvent = new TitleEvent(mock(Widget.class));
    titleEvent.title = "Arbitrary HTML Page Title";

    new AuthorizationCodeListener(loginBrowser).changed(titleEvent);

    verify(loginBrowser, never()).setAuthorizationCode(anyString());
    verify(loginBrowser, never()).logOutAndClose();
  }

  @Test
  public void testTitleListener_authorizationCodeTitle() {
    GoogleLoginBrowser loginBrowser = mock(GoogleLoginBrowser.class);
    TitleEvent titleEvent = new TitleEvent(mock(Widget.class));
    titleEvent.title = "Success code=fake_authorization_code";

    new AuthorizationCodeListener(loginBrowser).changed(titleEvent);

    verify(loginBrowser, times(1)).setAuthorizationCode(eq("fake_authorization_code"));
    verify(loginBrowser, times(1)).logOutAndClose();
  }
}
