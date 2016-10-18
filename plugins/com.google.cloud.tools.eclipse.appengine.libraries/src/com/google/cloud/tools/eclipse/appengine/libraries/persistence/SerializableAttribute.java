package com.google.cloud.tools.eclipse.appengine.libraries.persistence;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.JavaCore;

public class SerializableAttribute {

  private String name;
  private String value;

  public SerializableAttribute(String name, String value) {
    this.name = name;
    this.value = value;
  }
  
  public IClasspathAttribute toClasspathAttribute() {
    return JavaCore.newClasspathAttribute(name, value);
  }
}