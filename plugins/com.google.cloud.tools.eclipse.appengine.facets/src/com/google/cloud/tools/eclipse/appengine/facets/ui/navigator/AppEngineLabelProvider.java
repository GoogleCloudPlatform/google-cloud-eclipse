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

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineProjectElement;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineResourceElement;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DatastoreIndexesDescriptor;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineImages;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

public class AppEngineLabelProvider extends LabelProvider implements IStyledLabelProvider {
  private final ResourceManager resources;

  public AppEngineLabelProvider() {
    this(new LocalResourceManager(JFaceResources.getResources()));
  }

  @VisibleForTesting
  AppEngineLabelProvider(ResourceManager resources) {
    this.resources = resources;
  }

  @Override
  public String getText(Object element) {
    StyledString result = getStyledText(element);
    return result == null ? null : result.toString();
  }

  @Override
  public StyledString getStyledText(Object element) {
    if (element instanceof IProject && AppEngineContentProvider.isAppEngine((IProject) element)) {
      return getAppEngineProjectText((IProject) element);
    } else if (element instanceof AppEngineProjectElement) {
      return ((AppEngineProjectElement) element).getStyledLabel();
    } else if (element instanceof AppEngineResourceElement) {
      return ((AppEngineResourceElement) element).getStyledLabel();
    }
    return null; // continue on to the next label provider
  }

  @VisibleForTesting
  static StyledString getAppEngineProjectText(IProject project) {
    try {
      AppEngineProjectElement projectElement = AppEngineContentProvider.loadRepresentation(project);
      StyledString result = new StyledString(project.getName());
      String qualifier = getVersionTuple(projectElement);
      if (qualifier.length() > 0) {
        result.append(" [", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
        result.append(qualifier.toString(), StyledString.QUALIFIER_STYLER);
        result.append("]", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
      }
      return result;
    } catch (AppEngineException ex) {
      // ignore
    }
    return null; // carry onto the next label provider
  }

  /** Returns a <em>project:service:version</em> tuple from the App Engine descriptor. */
  @VisibleForTesting
  static String getVersionTuple(AppEngineProjectElement projectElement) {
    StringBuilder qualifier = new StringBuilder();

    String projectId = projectElement.getProjectId();
    if (!Strings.isNullOrEmpty(projectId)) {
      qualifier.append(projectId);
    }

    String serviceId = projectElement.getServiceId();
    if (!Strings.isNullOrEmpty(serviceId)) {
      if (qualifier.length() > 0) {
        qualifier.append(':');
      }
      qualifier.append(serviceId);
    }

    String projectVersion = projectElement.getProjectVersion();
    if (!Strings.isNullOrEmpty(projectVersion)) {
      if (qualifier.length() > 0) {
        qualifier.append(':');
      }
      qualifier.append(projectVersion);
    }
    return qualifier.toString();
  }

  @Override
  public Image getImage(Object element) {
    if (element instanceof IProject && AppEngineContentProvider.isAppEngine((IProject) element)) {
      return resources.createImage(AppEngineImages.APPENGINE_IMAGE_DESCRIPTOR);
    } else if (element instanceof DatastoreIndexesDescriptor) {
      return resources.createImage(SharedImages.DATASTORE_GREY_IMAGE_DESCRIPTOR);
    } else if (element instanceof AppEngineProjectElement
        || element instanceof AppEngineResourceElement) {
      // todo Get better images for these resource elements
      // CronDescriptor could be a timer/clock?
      // DenialOfServiceDescriptor could be a do-not-enter?
      // DispatchRoutingDescriptor could be a path fork?
      // TaskQueuesDescriptor could be a tree-like branch?
      return resources.createImage(AppEngineImages.APPENGINE_GREY_IMAGE_DESCRIPTOR);
    }
    return null;
  }

  @Override
  public void dispose() {
    super.dispose();
    if (resources != null) {
      resources.dispose();
    }
  }
}
