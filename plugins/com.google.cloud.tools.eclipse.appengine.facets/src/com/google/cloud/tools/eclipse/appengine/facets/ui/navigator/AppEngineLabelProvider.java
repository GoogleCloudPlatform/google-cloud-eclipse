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
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineResourceElement;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DatastoreIndexesDescriptor;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineImages;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.xml.sax.SAXException;

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
    if (element instanceof IProject && AppEngineContentProvider.isStandard((IProject) element)) {
      return getAppEngineStandardProjectText((IProject) element);
    } else if (element instanceof AppEngineResourceElement) {
      return ((AppEngineResourceElement) element).getStyledLabel();
    }
    return null; // continue on to the next label provider
  }

  @VisibleForTesting
  static StyledString getAppEngineStandardProjectText(IProject project) {
    IFile appEngineWeb = WebProjectUtil.findInWebInf(project, new Path("appengine-web.xml"));
    if (appEngineWeb == null || !appEngineWeb.exists()) {
      // continue on to the next
      return null;
    }
    StyledString result = new StyledString(project.getName());
    try (InputStream input = appEngineWeb.getContents()) {
      AppEngineDescriptor descriptor = AppEngineDescriptor.parse(input);
      String qualifier = getVersionTuple(descriptor);
      if (qualifier.length() > 0) {
        result.append(" [", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
        result.append(qualifier.toString(), StyledString.QUALIFIER_STYLER);
        result.append("]", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
      }
    } catch (IOException | CoreException | SAXException | AppEngineException ex) {
      // ignore
    }
    return result;
  }

  /** Returns a <em>project:service:version</em> tuple from the appengine-web descriptor. */
  @VisibleForTesting
  static String getVersionTuple(AppEngineDescriptor descriptor) throws AppEngineException {
    StringBuilder qualifier = new StringBuilder();
    if (descriptor != null) {
      if (!Strings.isNullOrEmpty(descriptor.getProjectId())) {
        qualifier.append(descriptor.getProjectId());
      }
      if (!Strings.isNullOrEmpty(descriptor.getServiceId())) {
        if (qualifier.length() > 0) {
          qualifier.append(':');
        }
        qualifier.append(descriptor.getServiceId());
      }
      if (!Strings.isNullOrEmpty(descriptor.getProjectVersion())) {
        if (qualifier.length() > 0) {
          qualifier.append(':');
        }
        qualifier.append(descriptor.getProjectVersion());
      }
    }
    return qualifier.toString();
  }

  @Override
  public Image getImage(Object element) {
    if (element instanceof IProject && AppEngineContentProvider.isStandard((IProject) element)) {
      return resources.createImage(AppEngineImages.APPENGINE_IMAGE_DESCRIPTOR);
    } else if (element instanceof DatastoreIndexesDescriptor) {
      return resources.createImage(SharedImages.DATASTORE_GREY_IMAGE_DESCRIPTOR);
    } else if (element instanceof AppEngineResourceElement) {
      // todo CronDescriptor should be a timer/clock?
      // todo DenialOfServiceDescriptor should be a do-not-enter?
      // todo DispatchRoutingDescriptor should be a path fork?
      // todo TaskQueuesDescriptor
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
