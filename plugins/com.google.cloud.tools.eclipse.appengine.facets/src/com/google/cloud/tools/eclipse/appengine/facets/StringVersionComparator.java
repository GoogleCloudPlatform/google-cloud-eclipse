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

package com.google.cloud.tools.eclipse.appengine.facets;

import java.util.Comparator;

/**
 * A {@code Comparator<String>} for the Faceted Project framework that compares versions that are
 * strings, like "JRE7" vs "JRE8", using the natural string ordering. Use the standard Faceted
 * Project {@link org.eclipse.wst.common.project.facet.core.DefaultVersionComparator}) comparator
 * for versions that have some numeric meaning (e.g., 1.2.0).
 */
public class StringVersionComparator implements Comparator<String> {

  // Since this comparator is usually specified via plugin.xml, one would normally
  // implement this as an IExecutableExtensionFactory that returns Ordering.natural().
  // But the fproj framework instantiates the specified class *directly* and so we
  // must actually implement the comparator.

  @Override
  public int compare(String o1, String o2) {
    return o1.compareTo(o2);
  }

}
