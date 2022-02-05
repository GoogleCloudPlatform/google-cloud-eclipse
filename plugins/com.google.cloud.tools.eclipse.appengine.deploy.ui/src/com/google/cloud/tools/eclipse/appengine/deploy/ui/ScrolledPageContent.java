/*
 * Copyright 2019 Google LLC.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;


public class ScrolledPageContent extends SharedScrolledComposite {

  public ScrolledPageContent(Composite parent) {
    this(parent, SWT.V_SCROLL | SWT.H_SCROLL);
  }

  public ScrolledPageContent(Composite parent, int style) {
    super(parent, style);
    setExpandHorizontal(true);
    setExpandVertical(true);

    Composite body = new Composite(this, SWT.NONE);
    body.setFont(parent.getFont());
    setContent(body);
  }

  public Composite getBody() {
    return (Composite) getContent();
  }
}
