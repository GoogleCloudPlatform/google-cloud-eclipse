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

import com.google.cloud.tools.eclipse.util.io.PathUtil;
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

  private static final String BINARY_REPO_RELATIVE_PREFIX = "BIN";
  private static final String SOURCE_REPO_RELATIVE_PREFIX = "SRC";

  private SerializableAccessRules[] accessRules;
  private String sourceAttachmentPath = "";
  private String path;
  private SerializableAttribute[] attributes;

  public SerializableClasspathEntry(IClasspathEntry entry, IPath baseDirectory,
      IPath sourceBaseDirectory) {
    setAttributes(entry.getExtraAttributes());
    setAccessRules(entry.getAccessRules());
    setSourcePath(
        relativizeSourcePath(entry.getSourceAttachmentPath(), baseDirectory, sourceBaseDirectory));
    setPath(PathUtil.relativizePath(entry.getPath(), baseDirectory).toString());
  }

  /**
   * Relativizes the source attachment path with respect to the base directories used to store
   * source and binary artifacts.
   * <p>
   * Tries to make the source attachment path relative to <code>sourceBaseDirectory</code>. If the
   * source attachment path is not relative to the sourceBaseDirectory, it will try to make it
   * relative to <code>baseDirectory</code>. If the source attachment path is not relative to
   * <code>baseDirectory</code> either, it will return it unchanged.
   * <p>
   * If the source attachment path is relative to either to <code>sourceBaseDirectory</code> or
   * <code>baseDirectory</code>, it will be prefixed with {@value #SOURCE_REPO_RELATIVE_PREFIX} or
   * {@value #BINARY_REPO_RELATIVE_PREFIX} respectively to inform the deserialization process.
   */
  private static IPath relativizeSourcePath(IPath sourceAttachmentPath, IPath baseDirectory,
      IPath sourceBaseDirectory) {
    if (sourceAttachmentPath == null) {
      return null;
    }
    IPath pathRelativeToSourceBase =
        PathUtil.relativizePathStrict(sourceAttachmentPath, sourceBaseDirectory);
    if (pathRelativeToSourceBase != null) {
      return new Path(SOURCE_REPO_RELATIVE_PREFIX).append(pathRelativeToSourceBase);
    } else {
      return new Path(BINARY_REPO_RELATIVE_PREFIX)
          .append(PathUtil.relativizePath(sourceAttachmentPath, baseDirectory));
    }
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
    if (sourceAttachmentPath == null) {
      this.sourceAttachmentPath = "";
    } else {
      this.sourceAttachmentPath = sourceAttachmentPath.toString();
    }
  }

  public IClasspathEntry toClasspathEntry(IPath baseDirectory, IPath sourceBaseDirectory) {
    IPath attachmentPath = sourceAttachmentPath.isEmpty() ? null
        : restoreSourcePath(baseDirectory, sourceBaseDirectory);
    return JavaCore.newLibraryEntry(PathUtil.makePathAbsolute(new Path(path), baseDirectory),
        attachmentPath, null, getAccessRules(), getAttributes(), true);
  }

  private IPath restoreSourcePath(IPath baseDirectory, IPath sourceBaseDirectory) {
    Path path = new Path(sourceAttachmentPath);
    if (path.segmentCount() > 0) {
      switch (path.segment(0)) {
        case SOURCE_REPO_RELATIVE_PREFIX:
          return PathUtil.makePathAbsolute(path.removeFirstSegments(1), sourceBaseDirectory);
        case BINARY_REPO_RELATIVE_PREFIX:
          return PathUtil.makePathAbsolute(path.removeFirstSegments(1), baseDirectory);
        default:
          // Unknown prefix, use path only if valid
          if (path.toFile().exists()) {
            return path;
          } else {
            return null;
          }
      }
    }
    return path;
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
