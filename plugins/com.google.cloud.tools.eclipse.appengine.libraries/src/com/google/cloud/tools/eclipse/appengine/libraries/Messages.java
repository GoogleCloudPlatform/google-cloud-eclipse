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

package com.google.cloud.tools.eclipse.appengine.libraries;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.google.cloud.tools.eclipse.appengine.libraries.messages"; //$NON-NLS-1$
  public static String AppEngineLibraryContainerResolverJobName;
  public static String ContainerPathInvalidFirstSegment;
  public static String ContainerPathNotTwoSegments;
  public static String CreateLibraryError;
  public static String LoadContainerFailed;
  public static String RepositoryCannotBeLocated;
  public static String RepositoryUriInvalid;
  public static String RepositoryUriNotAbsolute;
  public static String ResolveArtifactError;
  public static String TaskResolveArtifacts;
  public static String TaskResolveLibraries;
  public static String TaskResolveLibrariesError;
  public static String UnexpectedConfigurationElement;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
