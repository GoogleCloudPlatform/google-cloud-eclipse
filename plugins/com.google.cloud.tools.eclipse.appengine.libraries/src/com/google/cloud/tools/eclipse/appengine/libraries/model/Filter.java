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

package com.google.cloud.tools.eclipse.appengine.libraries.model;

import com.google.common.base.Preconditions;

public class Filter {

  private String pattern;
  private boolean exclude;

  /**
   * @param pattern expected format is the same as JDT's build path inclusion/exclusion filters.
   *
   * @see org.eclipse.jdt.core.IClasspathEntry#getExclusionPatterns()
   */
  public static Filter exclusionFilter(String pattern) {
    return new Filter(pattern, true /* exclude */);
  }

  /**
   * @param pattern expected format is the same as JDT's build path inclusion/exclusion filters.
   *
   * @see org.eclipse.jdt.core.IClasspathEntry#getInclusionPatterns()
   */
  public static Filter inclusionFilter(String pattern) {
    return new Filter(pattern, false /* exclude */);
  }

  private Filter(String pattern, boolean exclude) {
    Preconditions.checkNotNull(pattern, "pattern is null");
    Preconditions.checkArgument(!pattern.isEmpty(), "pattern is empty");
    this.pattern = pattern;
    this.exclude = exclude;
  }

  /**
   * @return a pattern in the format that is the same as JDT's build path inclusion/exclusion
   *         filters.
   *
   * @see org.eclipse.jdt.core.IClasspathEntry#getInclusionPatterns()
   */
  public String getPattern() {
    return pattern;
  }

  public boolean isExclude() {
    return exclude;
  }

}
