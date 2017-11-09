/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.preferences;

/**
 * A preference store for storing Key/Value pairs in some persistent storage.
 */
public interface PreferenceStore {
  /**
   * Retrieves the preference with the given name, or {@code Optional#absent()} if it is not
   * present.
   */
  String getOption(String optionName);

  /**
   * Set the preference with the given name to the given value. The value will not be written to
   * persistent storage until {@link #save()} is called.
   */
  void setOption(String optionName, String optionValue);

  /**
   * Saves all of the options that have been set in this {@link PreferenceStore}.
   */
  void save();
}

