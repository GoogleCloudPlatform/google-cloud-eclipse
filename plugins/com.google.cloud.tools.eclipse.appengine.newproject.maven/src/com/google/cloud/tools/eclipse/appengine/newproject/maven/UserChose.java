/*
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
 */

package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;

class UserChose implements KeyListener {

  private boolean userChosePackageName = false;  
	  
  boolean userChosePackageName() {
    return userChosePackageName;
  }

  @Override
  public void keyPressed(KeyEvent event) {
  }

  @Override
  public void keyReleased(KeyEvent event) {
    // filter out navigation keys
    if (event.character == '\t' || event.character == '\r' || event.character == '\n') {
	  return;
    }
    if (event.keyCode == SWT.ESC || event.keyCode == SWT.ARROW_UP 
        || event.keyCode == SWT.ARROW_DOWN || event.keyCode == SWT.ARROW_LEFT 
        || event.keyCode == SWT.ARROW_RIGHT) {
      return;
    }
    this.userChosePackageName = true;
  }

}