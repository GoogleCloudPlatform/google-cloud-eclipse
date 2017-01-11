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

import com.google.cloud.tools.eclipse.appengine.deploy.DeployErrorMessageProvider;
import com.google.cloud.tools.eclipse.sdk.OutputCollectorOutputLineListener;
import com.google.common.base.Joiner;
import java.util.List;

public class ConsoleOutputBasedDeployErrorMessageProvider implements DeployErrorMessageProvider {

  private OutputCollectorOutputLineListener outputLineListener;

  public ConsoleOutputBasedDeployErrorMessageProvider(OutputCollectorOutputLineListener outputLineListener) {
    this.outputLineListener = outputLineListener;
  }

  @Override
  public String getErrorMessage() {
    List<String> messages = outputLineListener.getCollectedMessages();
    if (!messages.isEmpty()) {
      return Joiner.on('\n').join(messages);
    } else {
      return null;
    }
  }
}
