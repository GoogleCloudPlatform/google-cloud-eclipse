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

import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/** Utility methods for creating App Engine configuration files for testing purposes. */
public class ConfigurationFileUtils {

  /** WebProjectUtil#createFileInWebInf() does not overwrite files. */
  public static IFile createInWebInf(IProject project, IPath path, String contents) {
    try {
      IFile previous = WebProjectUtil.findInWebInf(project, path);
      if (previous != null && previous.exists()) {
        previous.delete(true, null);
      }
      return WebProjectUtil.createFileInWebInf(
          project, path, new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)), null);
    } catch (CoreException ex) {
      fail(ex.toString());
      /*NOTREACHED*/
      return null;
    }
  }

  public static IFile createEmptyCronXml(IProject project) {
    return createInWebInf(
        project,
        new Path("cron.xml"), // $NON-NLS-1$
        "<cronentries/>"); // $NON-NLS-1$
  }

  public static IFile createEmptyDispatchXml(IProject project) {
    return createInWebInf(
        project,
        new Path("dispatch.xml"), // $NON-NLS-1$
        "<dispatch-entries/>"); // $NON-NLS-1$
  }

  public static IFile createEmptyDosXml(IProject project) {
    return createInWebInf(
        project,
        new Path("dos.xml"), // $NON-NLS-1$
        "<blacklistentries/>"); // $NON-NLS-1$
  }

  public static IFile createEmptyQueueXml(IProject project) {
    return createInWebInf(
        project,
        new Path("queue.xml"), // $NON-NLS-1$
        "<queue-entries/>"); // $NON-NLS-1$
  }

  public static IFile createEmptyDatastoreIndexesXml(IProject project) {
    return createInWebInf(
        project,
        new Path("datastore-indexes.xml"), // $NON-NLS-1$
        "<datastore-indexes/>"); // $NON-NLS-1$
  }

  public static IFile createAppEngineWebXml(IProject project, String serviceId) {
    String contents =
        Strings.isNullOrEmpty(serviceId)
            ? "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'/>" // $NON-NLS-1$
            : "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'><service>" // $NON-NLS-1$
                + serviceId
                + "</service></appengine-web-app>"; // $NON-NLS-1$
    return createInWebInf(project, new Path("appengine-web.xml"), contents); // $NON-NLS-1$
  }
}
