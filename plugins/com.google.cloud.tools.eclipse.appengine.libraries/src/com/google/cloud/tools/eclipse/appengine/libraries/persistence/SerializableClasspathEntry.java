package com.google.cloud.tools.eclipse.appengine.libraries.persistence;

import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

public class SerializableClasspathEntry {

  private ArrayList<SerializableAccessRules> accessRules;
  private String sourceAttachmentPath;
  private String path;
  private ArrayList<SerializableAttribute> attributes;

  public void setAttributes(IClasspathAttribute[] extraAttributes) {
    attributes = new ArrayList<>(extraAttributes.length);
    for (IClasspathAttribute attribute : extraAttributes) {
      attributes.add(new SerializableAttribute(attribute.getName(), attribute.getValue()));
    }
  }

  public void setPath(IPath path) {
    this.path = path.toOSString();
  }

  public void setAccessRules(IAccessRule[] accessRules) {
    this.accessRules = new ArrayList<>(accessRules.length);
    for (IAccessRule rule : accessRules) {
      this.accessRules.add(new SerializableAccessRules(rule.getKind(), rule.getPattern()));
    }
  }

  public void setSourcePath(IPath sourceAttachmentPath) {
    this.sourceAttachmentPath = sourceAttachmentPath.toOSString();
  }

  public IClasspathEntry toClasspathEntry() {
    return JavaCore.newLibraryEntry(new Path(path),
                                    new Path(sourceAttachmentPath),
                                    null,
                                    getAccessRules(accessRules),
                                    getAttributes(attributes),
                                    true);
  }
  
  private IClasspathAttribute[] getAttributes(ArrayList<SerializableAttribute> attributes) {
    IClasspathAttribute[] classpathAttributes = new IClasspathAttribute[attributes.size()];
    for (int i = 0; i < attributes.size(); i++) {
      SerializableAttribute serializableAttribute = attributes.get(i);
      classpathAttributes[i] = serializableAttribute.toClasspathAttribute();
    }
    return classpathAttributes;
  }

  private IAccessRule[] getAccessRules(ArrayList<SerializableAccessRules> accessRules) {
    IAccessRule[] rules = new IAccessRule[accessRules.size()];
    for (int i = 0; i < accessRules.size(); i++) {
      SerializableAccessRules serializableAccessRules = accessRules.get(i);
      rules[i] = serializableAccessRules.toAccessRule();
    }
    return rules;
  }
}