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

package com.google.cloud.tools.eclipse.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class MappedNamespaceContext implements NamespaceContext {

  private final Map<String, String> prefixMapping = new HashMap<>();

  public MappedNamespaceContext() {
    prefixMapping.put("xml", XMLConstants.XML_NS_URI);
  }

  public MappedNamespaceContext(String prefix, String namespaceUri) {
    this();
    addMapping(prefix, namespaceUri);
  }

  public MappedNamespaceContext addMapping(String prefix, String namespaceUri) {
    if (prefix == null) {
      throw new IllegalArgumentException("Prefix can't be null");
    } else if (namespaceUri == null) {
      throw new IllegalArgumentException("Namespace URI can't be null");
    } else if (prefix.equals("xml")) {
      if (namespaceUri.equals(XMLConstants.XML_NS_URI)) {
        return this;  // Matches the correct URI; just return.
      } else {
        throw new IllegalArgumentException("Cannot redefine the 'xml' prefix");
      }
    }

    prefixMapping.put(prefix, namespaceUri);
    return this;
  }

  @Override
  public String getNamespaceURI(String prefix) {
    if (prefix == null) {
      throw new IllegalArgumentException("Prefix can't be null");
    }
    return prefixMapping.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
  }

  @Override
  public String getPrefix(String namespaceURI) {
    Iterator<String> iterator = getPrefixes(namespaceURI);
    return iterator.hasNext() ? iterator.next() : null;
  }

  @Override
  public Iterator<String> getPrefixes(String namespaceURI) {
    if (namespaceURI == null) {
      throw new IllegalArgumentException("Namespace URI can't be null");
    }
    List<String> prefixes = prefixMapping.entrySet().stream()
        .filter(entry -> entry.getValue().equals(namespaceURI))
        .map(entry -> entry.getKey())
        .collect(Collectors.toList());
    return prefixes.iterator();
  }
}
