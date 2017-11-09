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

package com.google.cloud.tools.eclipse.swtbot;

import com.google.common.base.Preconditions;

import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;

/**
 * Used to set the general SWTBot timeout. This class was originally to be thrown away, except that
 * SWTBot doesn't support a nice mechanism for temporarily changing the timeout.
 * 
 * Note that this is a "static" class, so it is not thread-safe. The pattern of usage is to call
 * {@link #setTimeout(long)} to set the general SWTBot timeout, and then call
 * {@link #resetTimeout()} to reset the SWTBot timeout back to it's original value. You should never
 * call {@link #setTimeout(long)} more than once without a call to {@link #resetTimeout()} in
 * between.
 */
public class SwtBotTimeoutManager {

  /* Milliseconds */
  private static final int TYPICAL_TIMEOUT = 30000;

  private static long UNSET_TIMEOUT_VALUE = -1;

  private static long oldTimeoutSwtPrefs = UNSET_TIMEOUT_VALUE;

  /**
   * Reset the timeout value back to what it was before
   * {@link #setTimeout(long)} was called.
   */
  public static void resetTimeout() {
    setSwtBotPrefsTimeoutFieldValue(oldTimeoutSwtPrefs);
  }

  /**
   * Set the SWTBot timeout to a value we've found suitable for our set of tests
   * running on our test machines.
   */
  public static void setTimeout() {
    setTimeout(TYPICAL_TIMEOUT);
  }

  /**
   * Set the SWTBot timeout value.
   * 
   * @param timeout the timeout value, in milliseconds
   */
  public static void setTimeout(long timeout) {
    Preconditions.checkArgument(timeout > 0);
    oldTimeoutSwtPrefs = getSwtBotPrefsTimeoutFieldValue();
    setSwtBotPrefsTimeoutFieldValue(timeout);
  }

  private static long getSwtBotPrefsTimeoutFieldValue() {
    return SWTBotPreferences.TIMEOUT;
  }

  private static void setSwtBotPrefsTimeoutFieldValue(long timeout) {
    if (timeout == UNSET_TIMEOUT_VALUE) {
      return;
    }
    SWTBotPreferences.TIMEOUT = timeout;
  }
}
