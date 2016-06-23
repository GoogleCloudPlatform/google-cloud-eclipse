package com.google.cloud.tools.eclipse.appengine.sdk;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class AppEngineSDKClasspathInitializer extends ClasspathContainerInitializer {

  @Override
  public void initialize(final IPath containerPath, IJavaProject project) throws CoreException {
    if (containerPath.segmentCount() > 0 &&
        containerPath.segment(0).equals(AppEngineSDKClasspathContainer.CONTAINER_ID)) {
      IClasspathContainer container = new AppEngineSDKClasspathContainer();
      JavaCore.setClasspathContainer(new Path(AppEngineSDKClasspathContainer.CONTAINER_ID),
                                     new IJavaProject[]{ project },
                                     new IClasspathContainer[]{ container },
                                     null /* progressMonitor */);
    }
  }

}
