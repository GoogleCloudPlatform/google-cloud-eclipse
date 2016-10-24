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

package com.google.cloud.tools.eclipse.appengine.libraries.persistence;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

/**
 * Represents a {@link IClasspathEntry} in such a way that it can be easily transformed into JSON.
 */
public class SerializableClasspathEntry {

  private SerializableAccessRules[] accessRules;
  private String sourceAttachmentPath;
  private String path;
  private SerializableAttribute[] attributes;

  public SerializableClasspathEntry(IClasspathEntry entry, IPath baseDirectory) {
    setAttributes(entry.getExtraAttributes());
    setAccessRules(entry.getAccessRules());
    setSourcePath(entry.getSourceAttachmentPath());
    setPath(entry.getPath(), baseDirectory);
  }

  private void setPath(IPath path, IPath baseDirectory) {
    java.nio.file.Path base = baseDirectory.toFile().toPath().toAbsolutePath();
    java.nio.file.Path child = path.toFile().toPath().toAbsolutePath();
    if (child.startsWith(base)) {
      this.path = base.relativize(child).toString();
    } else {
      this.path = path.toString();
    }
  }

  public void setAttributes(IClasspathAttribute[] extraAttributes) {
    attributes = new SerializableAttribute[extraAttributes.length];
    for (int i = 0; i < extraAttributes.length; i++) {
      IClasspathAttribute attribute = extraAttributes[i];
      attributes[i] = new SerializableAttribute(attribute);
    }
  }

  public void setAccessRules(IAccessRule[] accessRules) {
    this.accessRules = new SerializableAccessRules[accessRules.length];
    for (int i = 0; i < accessRules.length; i++) {
      IAccessRule rule = accessRules[i];
      this.accessRules[i] = new SerializableAccessRules(rule);
    }
  }

  public void setSourcePath(IPath sourceAttachmentPath) {
    this.sourceAttachmentPath = sourceAttachmentPath.toOSString();
  }

  public IClasspathEntry toClasspathEntry(IPath baseDirectory) {
    return JavaCore.newLibraryEntry(baseDirectory.append(path),
                                    new Path(sourceAttachmentPath),
                                    null,
                                    getAccessRules(),
                                    getAttributes(),
                                    true);
  }

  private IClasspathAttribute[] getAttributes() {
    IClasspathAttribute[] classpathAttributes = new IClasspathAttribute[attributes.length];
    for (int i = 0; i < attributes.length; i++) {
      classpathAttributes[i] = attributes[i].toClasspathAttribute();
    }
    return classpathAttributes;
  }

  private IAccessRule[] getAccessRules() {
    IAccessRule[] rules = new IAccessRule[accessRules.length];
    for (int i = 0; i < accessRules.length; i++) {
      rules[i] = accessRules[i].toAccessRule();
    }
    return rules;
  }
}
