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

package com.google.cloud.tools.eclipse.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

public class AppEngineDescriptorTest {

  private static final String TEST_VERSION = "fooVersion";
  private static final String TEST_ID = "fooId";

  private static final String XML_SUFFIX = "</appengine-web-app>";
  private static final String XML_DECLARATION = "<?xml version='1.0' encoding='utf-8'?>"
                                              + "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>";
  private static final String XML_DECLARATION_WITH_INVALID_NS =
      "<?xml version='1.0' encoding='utf-8'?><appengine-web-app xmlns='http://foo.bar.com/ns/42'>";
  private static final String PROJECT_ID = "<application>" + TEST_ID + "</application>";
  private static final String VERSION = "<version>" + TEST_VERSION + "</version>";
  private static final String COMMENT = "<!-- this is a test comment -->";
  private static final String COMMENT_AFTER_VERSION = "<version>" + TEST_VERSION + COMMENT + "</version>";
  private static final String COMMENT_BEFORE_VERSION = "<version>" + COMMENT + TEST_VERSION + "</version>";
  private static final String SERVICE = "<service>" + TEST_ID + "</service>";
  private static final String MODULE = "<module>" + TEST_ID + "</module>";
  
  private static final String XML_WITHOUT_PROJECT_ID = XML_DECLARATION + XML_SUFFIX;
  private static final String XML_WITHOUT_VERSION = XML_DECLARATION + PROJECT_ID + XML_SUFFIX;
  private static final String XML_WITH_VERSION_AND_PROJECT_ID = XML_DECLARATION + PROJECT_ID + VERSION + XML_SUFFIX;
  private static final String XML_WITH_COMMENT_BEFORE_VERSION =
      XML_DECLARATION + PROJECT_ID + COMMENT_BEFORE_VERSION + XML_SUFFIX;
  private static final String XML_WITH_COMMENT_AFTER_VERSION =
      XML_DECLARATION + PROJECT_ID + COMMENT_AFTER_VERSION + XML_SUFFIX;
  private static final String XML_WITH_VERSION_AND_PROJECT_ID_WRONG_NS =
      XML_DECLARATION_WITH_INVALID_NS + PROJECT_ID + VERSION + XML_SUFFIX;

  @Test
  public void testParse_noProjectId() throws IOException, CoreException {
    File xml = createFileWithContent(XML_WITHOUT_PROJECT_ID);

    AppEngineDescriptor descriptor = new AppEngineDescriptor();
    descriptor.parse(xml);
    assertNull(descriptor.getProjectId());
  }

  @Test
  public void testParse_noVersion() throws IOException, CoreException {
    File xml = createFileWithContent(XML_WITHOUT_VERSION);

    AppEngineDescriptor descriptor = new AppEngineDescriptor();
    descriptor.parse(xml);
    assertThat(descriptor.getProjectId(), is(TEST_ID));
    assertNull(descriptor.getProjectVersion());
  }

  @Test
  public void testParse_properXml() throws IOException, CoreException {
    File xml = createFileWithContent(XML_WITH_VERSION_AND_PROJECT_ID);

    AppEngineDescriptor descriptor = new AppEngineDescriptor();
    descriptor.parse(xml);
    assertThat(descriptor.getProjectId(), is(TEST_ID));
    assertThat(descriptor.getProjectVersion(), is(TEST_VERSION));
  }

  @Test
  public void testParse_xmlWithCommentBeforeValue() throws IOException, CoreException {
    File xml = createFileWithContent(XML_WITH_COMMENT_BEFORE_VERSION);

    AppEngineDescriptor descriptor = new AppEngineDescriptor();
    descriptor.parse(xml);
    assertThat(descriptor.getProjectId(), is(TEST_ID));
    assertThat(descriptor.getProjectVersion(), is(TEST_VERSION));
  }

  @Test
  public void testParse_xmlWithCommentAfterValue() throws IOException, CoreException {
    File xml = createFileWithContent(XML_WITH_COMMENT_AFTER_VERSION);

    AppEngineDescriptor descriptor = new AppEngineDescriptor();
    descriptor.parse(xml);
    assertThat(descriptor.getProjectId(), is(TEST_ID));
    assertThat(descriptor.getProjectVersion(), is(TEST_VERSION));
  }

  
  @Test
  public void testParse_xmlWithInvalidNamespace() throws IOException, CoreException {
    File xml = createFileWithContent(XML_WITH_VERSION_AND_PROJECT_ID_WRONG_NS);

    AppEngineDescriptor descriptor = new AppEngineDescriptor();
    descriptor.parse(xml);
    assertNull(descriptor.getProjectId());
    assertNull(descriptor.getProjectVersion());
  }

  public void testService_noContent() throws IOException, CoreException {
    File xml = createFileWithContent(XML_DECLARATION + XML_SUFFIX);

    AppEngineDescriptor descriptor = new AppEngineDescriptor();
    descriptor.parse(xml);
    assertNull(descriptor.getServiceId());
  }

  public void testService_service() throws IOException, CoreException {
    File xml = createFileWithContent(XML_DECLARATION + SERVICE + XML_SUFFIX);

    AppEngineDescriptor descriptor = new AppEngineDescriptor();
    descriptor.parse(xml);
    assertThat(descriptor.getServiceId(), is(TEST_VERSION));
  }

  public void testService_module() throws IOException, CoreException {
    File xml = createFileWithContent(XML_DECLARATION + MODULE + XML_SUFFIX);

    AppEngineDescriptor descriptor = new AppEngineDescriptor();
    descriptor.parse(xml);
    assertThat(descriptor.getServiceId(), is(TEST_VERSION));
  }

  private File createFileWithContent(String xmlWithoutProjectId) throws IOException {
    File tempFile = File.createTempFile(getClass().getName(), null);
    tempFile.deleteOnExit();
    try (OutputStreamWriter streamWriter = new OutputStreamWriter(new FileOutputStream(tempFile))) {
      streamWriter.write(xmlWithoutProjectId);
    }
    return tempFile;
  }

}
