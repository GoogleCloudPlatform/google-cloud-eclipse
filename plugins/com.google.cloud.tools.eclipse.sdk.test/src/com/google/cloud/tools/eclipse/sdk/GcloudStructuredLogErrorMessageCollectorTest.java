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

package com.google.cloud.tools.eclipse.sdk;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import org.junit.Test;

public class GcloudStructuredLogErrorMessageCollectorTest {

  private final GcloudStructuredLogErrorMessageCollector errorMessageCollector =
      new GcloudStructuredLogErrorMessageCollector();

  @Test
  public void testInitiallyEmpty() {
    assertThat(errorMessageCollector.getErrorMessages(), empty());
  }

  @Test
  public void testIgnoreUnstructuredOutput() {
    errorMessageCollector.onOutputLine("non-JSON line");
    assertThat(errorMessageCollector.getErrorMessages(), empty());
  }

  @Test
  public void testIgnoreIrrelevantJsonLine() {
    errorMessageCollector.onOutputLine("{ 'key': 'value' }");
    assertThat(errorMessageCollector.getErrorMessages(), empty());
  }

  @Test
  public void testCaseInSensitiveVerbosity() {
    errorMessageCollector.onOutputLine("{ 'verbosity': 'eRrOr', 'message': 'OMG!' }");
    assertThat(errorMessageCollector.getErrorMessages(), equalTo(Arrays.asList("OMG!")));
  }

  @Test
  public void testNullMessage() {
    errorMessageCollector.onOutputLine("{ 'verbosity': 'error' }");
    assertThat(errorMessageCollector.getErrorMessages(),
        equalTo(Arrays.asList("no error message provided")));
  }

  @Test
  public void testEmptyMessage() {
    errorMessageCollector.onOutputLine("{ 'verbosity': 'error', 'message': '  ' }");
    assertThat(errorMessageCollector.getErrorMessages(),
        equalTo(Arrays.asList("no error message provided")));
  }

  @Test
  public void testIgonreNonErrorVerbosity() {
    errorMessageCollector.onOutputLine("{ 'verbosity': 'warning', 'message': 'ignored' }");
    assertThat(errorMessageCollector.getErrorMessages(), empty());
  }

  @Test
  public void testMultipleLines() {
    errorMessageCollector.onOutputLine("1st normal output");
    errorMessageCollector.onOutputLine("{ 'verbosity': 'Error', 'message': '1st error log' }");
    errorMessageCollector.onOutputLine("2nd normal output");
    errorMessageCollector.onOutputLine("{ 'verbosity': 'ERROR', 'message': '2nd error log' }");
    errorMessageCollector.onOutputLine("3rd normal output");
    errorMessageCollector.onOutputLine("4th normal output");
    errorMessageCollector.onOutputLine("{ 'verbosity': 'debug', 'message': 'uninteresting log' }");
    errorMessageCollector.onOutputLine("{ 'verbosity': 'error', 'message': '3rd error log' }");
    assertThat(errorMessageCollector.getErrorMessages(), equalTo(Arrays.asList(
        "1st error log", "2nd error log", "3rd error log")));
  }
}
