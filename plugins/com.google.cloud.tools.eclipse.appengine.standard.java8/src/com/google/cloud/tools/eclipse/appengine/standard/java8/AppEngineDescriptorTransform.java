/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.standard.java8;

import com.google.cloud.tools.eclipse.util.Xslt;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

/**
 * Utility class for adding and removing the {@code <runtime>java8</runtime>} to/from an
 * {@code appengine-web.xml}.
 */
public class AppEngineDescriptorTransform {
  private static final Logger logger =
      Logger.getLogger(AppEngineDescriptorTransform.class.getName());

  public static void removeJava8Runtime(IFile descriptor) {
    URL xslTemplate =
        AppEngineDescriptorTransform.class.getResource("/xslt/removeJava8Runtime.xsl");
    try {
      logger.fine("Removing runtime:java8 from " + descriptor);
      Xslt.transformInPlace(descriptor, xslTemplate);
    } catch (IOException | CoreException | TransformerException ex) {
      logger.log(Level.SEVERE, "Unable to remove runtime:java8 from " + descriptor, ex);
    }
  }

  public static void addJava8Runtime(IFile descriptor) {
    URL xslTemplate = AppEngineDescriptorTransform.class.getResource("/xslt/addJava8Runtime.xsl");
    try {
      logger.fine("Adding runtime:java8 from " + descriptor);
      Xslt.transformInPlace(descriptor, xslTemplate);
    } catch (IOException | CoreException | TransformerException ex) {
      logger.log(Level.SEVERE, "Unable to add runtime:java8 to " + descriptor, ex);
    }
  }

}
