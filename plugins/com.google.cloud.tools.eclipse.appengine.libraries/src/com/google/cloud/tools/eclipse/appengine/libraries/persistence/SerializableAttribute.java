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

package com.google.cloud.tools.eclipse.appengine.libraries.persistence;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.JavaCore;

/**
 * Represents a {@link IClasspathAttribute} in such a way that it can be easily transformed into JSON.
 */
public class SerializableAttribute {

  private String name;
  private String value;

  public SerializableAttribute(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public IClasspathAttribute toClasspathAttribute() {
    return JavaCore.newClasspathAttribute(name, value);
  }
}
