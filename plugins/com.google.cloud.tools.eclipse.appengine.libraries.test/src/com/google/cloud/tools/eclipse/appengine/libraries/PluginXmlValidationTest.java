package com.google.cloud.tools.eclipse.appengine.libraries;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.Test;
import org.xml.sax.SAXException;

public class PluginXmlValidationTest {

  @Test
  public void validatePluginXml() throws SAXException, IOException {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema(new File("xsd/com.google.cloud.tools.eclipse.appengine.libraries.xsd"));
    Source source = new StreamSource(new File("../com.google.cloud.tools.eclipse.appengine.libraries/plugin.xml"));
    schema.newValidator().validate(source);
  }
}
