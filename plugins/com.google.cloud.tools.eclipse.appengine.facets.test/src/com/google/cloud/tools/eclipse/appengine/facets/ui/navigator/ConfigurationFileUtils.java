/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.facets.ui.navigator;

import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/** Testing utility methods for creating App Engine configuration files for testing purposes. */
public class ConfigurationFileUtils {
  public static IFile createEmptyCronXml(IProject project) {
    return createFileInWebInf(project, new Path("cron.xml"), "<cronentries/>");
  }

  public static IFile createEmptyDispatchXml(IProject project) {
    return createFileInWebInf(project, new Path("dispatch.xml"), "<dispatch-entries/>");
  }

  public static IFile createEmptyDosXml(IProject project) {
    return createFileInWebInf(project, new Path("dos.xml"), "<blacklistentries/>");
  }

  public static IFile createEmptyQueueXml(IProject project) {
    return createFileInWebInf(project, new Path("queue.xml"), "<queue-entries/>");
  }

  public static IFile createEmptyDatastoreIndexesXml(IProject project) {
    return createFileInWebInf(project, new Path("datastore-indexes.xml"), "<datastore-indexes/>");
  }

  public static IFile createAppEngineWebXml(IProject project, String serviceId) {
    String contents =
        Strings.isNullOrEmpty(serviceId)
            ? "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'/>"
            : "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
                + "<service>"
                + serviceId
                + "</service>"
                + "</appengine-web-app>";
    return createFileInWebInf(project, new Path("appengine-web.xml"), contents);
  }

  // eliminate boilerplate
  private static IFile createFileInWebInf(IProject project, IPath path, String contents) {
    try {
      ByteArrayInputStream stream =
          new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
      return WebProjectUtil.createFileInWebInf(project, path, stream, true, null);
    } catch (CoreException ex) {
      throw new AssertionError(ex.toString(), ex);
    }
  }
}
