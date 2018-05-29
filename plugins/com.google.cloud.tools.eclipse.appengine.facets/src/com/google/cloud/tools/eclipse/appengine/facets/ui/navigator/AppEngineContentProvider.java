/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.facets.ui.navigator;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineWebDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.CronDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DatastoreIndexesDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DenialOfServiceDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DispatchRoutingDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.TaskQueuesDescriptor;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.xml.sax.SAXException;

public class AppEngineContentProvider implements ITreeContentProvider {
  private static final Object[] EMPTY_ARRAY = new Object[0];
  private static final Logger logger = Logger.getLogger(AppEngineContentProvider.class.getName());

  @Override
  public Object[] getElements(Object inputElement) {
    return getChildren(inputElement);
  }

  @Override
  public boolean hasChildren(Object element) {
    return getProject(element) != null || element instanceof AppEngineWebDescriptor;
  }

  @Override
  public Object[] getChildren(Object parentElement) {
    if (parentElement instanceof AppEngineWebDescriptor) {
      List<Object> contents = new ArrayList<>();
      AppEngineWebDescriptor webElement = (AppEngineWebDescriptor) parentElement;
      try {
        AppEngineDescriptor descriptor = webElement.getDescriptor();
        // ancillary config files are only taken from the default module
        if (descriptor.getServiceId() == null || "default".equals(descriptor.getServiceId())) {
          addCron(webElement.getProject(), contents);
          addDatastoreIndexes(webElement.getProject(), contents);
          addTaskQueues(webElement.getProject(), contents);
          addDenialOfService(webElement.getProject(), contents);
          addDispatch(webElement.getProject(), contents);
        }
        return contents.toArray();
      } catch (AppEngineException ex) {
        IPath path = webElement.getFile().getFullPath();
        logger.log(Level.WARNING, "Unable to parse " + path, ex);
      }
    }
    IProject project = getProject(parentElement);
    if (project != null && isStandard(project)) {
      IFile appEngineWebDescriptorFile =
          WebProjectUtil.findInWebInf(project.getProject(), new Path("appengine-web.xml"));
      if (appEngineWebDescriptorFile != null && appEngineWebDescriptorFile.exists()) {
        try (InputStream input = appEngineWebDescriptorFile.getContents()) {
          AppEngineDescriptor descriptor = AppEngineDescriptor.parse(input);
          return new Object[] {
            new AppEngineWebDescriptor(project, appEngineWebDescriptorFile, descriptor)
          };
        } catch (CoreException | SAXException | IOException ex) {
          IPath path = appEngineWebDescriptorFile.getFullPath();
          logger.log(Level.WARNING, "Unable to parse " + path, ex);
        }
      }
    }
    return EMPTY_ARRAY;
  }

  /** Add a {@code cron.xml} element if found */
  private void addCron(IProject project, List<Object> contents) {
    IFile cronXml = WebProjectUtil.findInWebInf(project.getProject(), new Path("cron.xml"));
    if (cronXml != null && cronXml.exists()) {
      contents.add(new CronDescriptor(project, cronXml));
    }
  }

  /** Add a {@code datastore-indexes.xml} element if found */
  private void addDatastoreIndexes(IProject project, List<Object> contents) {
    IFile datastoreIndexes =
        WebProjectUtil.findInWebInf(project.getProject(), new Path("datastore-indexes.xml"));
    if (datastoreIndexes != null && datastoreIndexes.exists()) {
      contents.add(new DatastoreIndexesDescriptor(project, datastoreIndexes));
    }
  }

  /** Add a {@code dispatch.xml} element if found. */
  private void addDispatch(IProject project, List<Object> contents) {
    IFile dispatchXml = WebProjectUtil.findInWebInf(project.getProject(), new Path("dispatch.xml"));
    if (dispatchXml != null && dispatchXml.exists()) {
      contents.add(new DispatchRoutingDescriptor(project, dispatchXml));
    }
  }

  /** Add a {@code dos.xml} element if found. */
  private void addDenialOfService(IProject project, List<Object> contents) {
    IFile dosXml = WebProjectUtil.findInWebInf(project.getProject(), new Path("dos.xml"));
    if (dosXml != null && dosXml.exists()) {
      contents.add(new DenialOfServiceDescriptor(project, dosXml));
    }
  }

  /** Add a {@code queue.xml} element if found */
  private void addTaskQueues(IProject project, List<Object> contents) {
    IFile queueXml = WebProjectUtil.findInWebInf(project.getProject(), new Path("queue.xml"));
    if (queueXml != null && queueXml.exists()) {
      contents.add(new TaskQueuesDescriptor(project, queueXml));
    }
  }

  @Override
  public Object getParent(Object element) {
    return null;
  }

  /** Return {@code true} if the project is an App Engine Standard project. */
  static boolean isStandard(IProject project) {
    Preconditions.checkNotNull(project);
    try {
      IFacetedProject facetedProject = ProjectFacetsManager.create(project);
      return facetedProject != null && AppEngineStandardFacet.hasFacet(facetedProject);
    } catch (CoreException ex) {
      logger.log(Level.INFO, "Project is not faceted", ex);
      return false;
    }
  }

  /** Try to get a project from the given element, return {@code null} otherwise. */
  private static IProject getProject(Object inputElement) {
    if (inputElement instanceof IFacetedProject) {
      return ((IFacetedProject) inputElement).getProject();
    } else if (inputElement instanceof IProject) {
      return (IProject) inputElement;
    }
    return null;
  }
}
