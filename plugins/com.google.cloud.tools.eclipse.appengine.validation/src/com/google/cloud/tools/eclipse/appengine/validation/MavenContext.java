package com.google.cloud.tools.eclipse.appengine.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;

import com.google.common.base.Preconditions;

public class MavenContext implements NamespaceContext {

  @Override
  public String getNamespaceURI(String prefix) {
    return "http://maven.apache.org/POM/4.0.0";
  }

  @Override
  public String getPrefix(String namespaceURI) {
    return "prefix";
  }

  @Override
  public Iterator<String> getPrefixes(String namespaceURI) {
    Preconditions.checkNotNull(namespaceURI);
    Set<String> set = new HashSet<>(Arrays.asList("prefix"));
    return set.iterator();
  }

}
