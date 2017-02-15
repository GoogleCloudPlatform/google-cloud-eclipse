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

package com.google.cloud.tools.eclipse.util.templates.appengine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Preconditions;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class AppEngineTemplateUtility {
  public static final String APPENGINE_WEB_XML_TEMPLATE = "appengine-web.xml.ftl";
  public static final String HELLO_APPENGINE_TEMPLATE = "HelloAppEngine.java.ftl";
  public static final String INDEX_HTML_TEMPLATE = "index.html.ftl";
  public static final String WEB_XML_TEMPLATE = "web.xml.ftl";
  public static final String HELLO_APPENGINE_TEST_TEMPLATE = "HelloAppEngineTest.java.ftl";
  public static final String MOCK_HTTPSERVLETRESPONSE_TEMPLATE = "MockHttpServletResponse.java.ftl";

  private static Configuration configuration;

  public static void createFileContent(String outputFileLocation, String templateName, Map<String, String> dataMap)
      throws CoreException {
    Preconditions.checkNotNull(outputFileLocation, "output file is null");
    Preconditions.checkNotNull(templateName, "template name is null");
    Preconditions.checkNotNull(dataMap, "data map is null");

    try {
      if (configuration == null) {
        configuration = createConfiguration();
      }
      File outputFile = new File(outputFileLocation);
      Writer fileWriter = new FileWriter(outputFile);
      Template template = configuration.getTemplate(templateName);
      template.process(dataMap, fileWriter);
    } catch (IOException | TemplateException e) {
      throw new CoreException(StatusUtil.error(AppEngineTemplateUtility.class, e.getMessage()));
    }
  }

  private AppEngineTemplateUtility() {
  }

  private static Configuration createConfiguration() {
    Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
    cfg.setClassForTemplateLoading(AppEngineTemplateUtility.class, "/templates/appengine");
    cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    cfg.setLogTemplateExceptions(false);
    return cfg;
  }

}
