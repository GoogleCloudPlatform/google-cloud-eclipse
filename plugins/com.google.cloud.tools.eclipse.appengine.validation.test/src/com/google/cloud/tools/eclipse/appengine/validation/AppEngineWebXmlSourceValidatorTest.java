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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.IncrementalReporter;
import org.junit.Test;

public class AppEngineWebXmlSourceValidatorTest {

  private static final byte[] XML_NO_BANNED_ELEMENTS =
      "<test></test>".getBytes(StandardCharsets.UTF_8);
  private static final byte[] XML =
      "<application></application>".getBytes(StandardCharsets.UTF_8);
  private IncrementalReporter reporter = new IncrementalReporter(null /*progress monitor*/);
  
  @Test
  public void testValidate_noBannedElements() throws CoreException, IOException, ParserConfigurationException {
    AppEngineWebXmlSourceValidator validator = new AppEngineWebXmlSourceValidator();
    validator.validate(reporter, XML_NO_BANNED_ELEMENTS);
    assertTrue(reporter.getMessages().isEmpty());
  }
  
  @Test
  public void testValidate() throws CoreException, IOException, ParserConfigurationException {
    AppEngineWebXmlSourceValidator validator = new AppEngineWebXmlSourceValidator();
    validator.validate(reporter, XML);
    assertEquals(1, reporter.getMessages().size());
    reporter.removeAllMessages(validator);
    assertTrue(reporter.getMessages().isEmpty());
  }
}