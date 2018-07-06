/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import org.junit.Test;

public class AboutPropertiesTest {

  @Test
  public void testWellFormed() throws IOException {
    Properties properties = new Properties();
    try (InputStream in = readProperties()) {
      properties.load(in);
    }
  }

  private InputStream readProperties() throws IOException {
    String propertiesLocation = "../com.google.cloud.tools.eclipse.appengine.ui/about.properties";
    return Files.newInputStream(Paths.get(propertiesLocation));
  }
  
}
