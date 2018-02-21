/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.libraries;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

  /**
   * Listen for changes to Java project classpath containers. If our Google Cloud Libraries
   * container has been removed, then clean up any definition files.
   */
  private IElementChangedListener listener = new IElementChangedListener() {
    @Override
    public void elementChanged(ElementChangedEvent event) {
      visit(event.getDelta());
    }

    private void visit(IJavaElementDelta delta) {
      switch (delta.getElement().getElementType()) {
        case IJavaElement.JAVA_PROJECT:
          if ((delta.getFlags() & IJavaElementDelta.F_CLASSPATH_CHANGED) != 0) {
            final IJavaProject javaProject = (IJavaProject) delta.getElement();
            Job updateContainerStateJob = new WorkspaceJob("Updating Google Cloud libraries") {
              @Override
              public IStatus runInWorkspace(IProgressMonitor monitor) {
                BuildPath.checkLibraryList(javaProject, null);
                return Status.OK_STATUS;
              }
            };
            updateContainerStateJob.setSystem(true);
            updateContainerStateJob.schedule();
          }
          break;
        case IJavaElement.JAVA_MODEL:
          visitChildren(delta);
          break;
        default:
          break;
      }
    }

    private void visitChildren(IJavaElementDelta delta) {
      for (IJavaElementDelta childDelta : delta.getAffectedChildren()) {
        visit(childDelta);
      }
    }
  };

  @Override
  public void start(BundleContext context) {
    JavaCore.addElementChangedListener(listener, ElementChangedEvent.POST_CHANGE);
  }

  @Override
  public void stop(BundleContext context) {
    JavaCore.removeElementChangedListener(listener);
  }
}
