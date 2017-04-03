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

package com.google.cloud.tools.eclipse.appengine.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;

import com.google.common.base.Preconditions;

public class JavaContext implements NamespaceContext {

  @Override
  public String getNamespaceURI(String prefix) {
    return "http://java.sun.com/xml/ns/javaee";
  }

  @Override
  public String getPrefix(String namespaceUri) {
    return "prefix";
  }

  @Override
  public Iterator<String> getPrefixes(String namespaceUri) {
    Preconditions.checkNotNull(namespaceUri);
    Set<String> set = new HashSet<>(Arrays.asList("prefix"));
    return set.iterator();
  }

}
