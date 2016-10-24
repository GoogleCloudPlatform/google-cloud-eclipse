package com.google.cloud.tools.eclipse.appengine.libraries;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.google.cloud.tools.eclipse.appengine.libraries.messages"; //$NON-NLS-1$
  public static String ContainerPathInvalidFirstSegment;
  public static String LoadContainerFailed;
  public static String ContainerPathNotTwoSegments;
  public static String BundleContextNotFound;
  public static String TaskResolveLibraries;
  public static String TaskResolveLibrariesError;
  public static String TaskResolveArtifacts;
  public static String UnexpectedConfigurationElement;
  public static String CreateLibraryError;
  public static String ResolveArtifactError;
  public static String RepositoryUriNotAbsolute;
  public static String RepositoryUriInvalid;
  public static String RepositoryCannotBeLocated;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
