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
    setPath(relativizePath(entry.getPath(), baseDirectory));
  }

  private void setPath(String path) {
    this.path = path;
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
    return JavaCore.newLibraryEntry(makePathAbsolute(path, baseDirectory),
                                    new Path(sourceAttachmentPath),
                                    null,
                                    getAccessRules(),
                                    getAttributes(),
                                    true);
  }

  // if path is relative, it's appended to baseDirectory, otherwise unchanged
  private static IPath makePathAbsolute(String path, IPath baseDirectory) {
    IPath jarPath = new Path(path);
    if (jarPath.isAbsolute()) {
      return jarPath;
    } else {
      return baseDirectory.append(jarPath);
    }
  }

  // converts path to relative if it is under baseDirectory, otherwise uses original
  private static String relativizePath(IPath path, IPath baseDirectory) {
    java.nio.file.Path base = baseDirectory.toFile().toPath().toAbsolutePath();
    java.nio.file.Path child = path.toFile().toPath().toAbsolutePath();
    if (child.startsWith(base)) {
      return base.relativize(child).toString();
    } else {
      return path.toString();
    }
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
