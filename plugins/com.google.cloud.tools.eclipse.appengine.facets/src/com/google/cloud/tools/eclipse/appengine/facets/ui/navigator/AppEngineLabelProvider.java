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
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineResourceElement;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineWebDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.CronDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DatastoreIndexes;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DenialOfServiceDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.RequestDispatchDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.TaskQueuesDescriptor;
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
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.xml.sax.SAXException;

public class AppEngineLabelProvider extends LabelProvider implements IStyledLabelProvider {
  private LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources());

  @Override
  public String getText(Object element) {
    StyledString result = getStyledText(element);
    return result == null ? null : result.toString();
  }

  @Override
  public StyledString getStyledText(Object element) {
    if (element instanceof IProject) {
      IFacetedProject project = AppEngineContentProvider.getProject(element);
      if (project != null && AppEngineStandardFacet.hasFacet(project)) {
        StyledString str = new StyledString(((IProject) element).getName());
        IFile appEngineWebDescriptorFile =
            WebProjectUtil.findInWebInf(project.getProject(), new Path("appengine-web.xml"));
        if (appEngineWebDescriptorFile != null && appEngineWebDescriptorFile.exists()) {
          try (InputStream input = appEngineWebDescriptorFile.getContents()) {
            AppEngineDescriptor descriptor = AppEngineDescriptor.parse(input);
            StringBuilder qualifier = toPrettyString(descriptor);
            if (qualifier.length() > 0) {
              str.append(" (", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
              str.append(qualifier.toString(), StyledString.QUALIFIER_STYLER);
              str.append(")", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
            }
          } catch (IOException | CoreException | SAXException | AppEngineException ex) {
            // ignore
          }
        }
        return str;
      }
    } else if (element instanceof AppEngineWebDescriptor) {
      StyledString str = new StyledString("App Engine");
      str.append(" [standard", StyledString.QUALIFIER_STYLER);
      AppEngineDescriptor descriptor = ((AppEngineWebDescriptor) element).getDescriptor();
      try {
        StyledString qualifier = new StyledString(": ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
        qualifier.append(
            Strings.isNullOrEmpty(descriptor.getRuntime()) ? "java7" : descriptor.getRuntime(),
            StyledString.QUALIFIER_STYLER);
        qualifier.append("]", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
        str.append(qualifier);
      } catch (AppEngineException ex) {
        // ignored
      }
      return str;
    } else if (element instanceof CronDescriptor) {
      return new StyledString("Scheduled Tasks").append(" cron.xml", StyledString.QUALIFIER_STYLER);
    } else if (element instanceof DatastoreIndexes) {
      return new StyledString("Datastore Indexes").append(" datastore-indexes.xml",
          StyledString.QUALIFIER_STYLER);
    } else if (element instanceof DenialOfServiceDescriptor) {
      return new StyledString("Denial of Service Protection").append(" dos.xml",
          StyledString.QUALIFIER_STYLER);
    } else if (element instanceof RequestDispatchDescriptor) {
      return new StyledString("Dispatch Routing Rules").append(" dispatch.xml",
          StyledString.QUALIFIER_STYLER);
    } else if (element instanceof TaskQueuesDescriptor) {
      return new StyledString("Task Queue Definitions").append(" queue.xml",
          StyledString.QUALIFIER_STYLER);
    }
    return null; // continue on to the next label provider
  }

  @VisibleForTesting
  static StringBuilder toPrettyString(AppEngineDescriptor descriptor) throws AppEngineException {
    StringBuilder qualifier = new StringBuilder();
    if (!Strings.isNullOrEmpty(descriptor.getProjectId())) {
      qualifier.append(descriptor.getProjectId());
    }
    if(!Strings.isNullOrEmpty(descriptor.getServiceId())) {
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
    return qualifier;
  }

  @Override
  public Image getImage(Object element) {
    if (element instanceof IProject) {
      IFacetedProject project = AppEngineContentProvider.getProject(element);
      if (project != null && AppEngineStandardFacet.hasFacet(project)) {
        return resources.createImage(AppEngineImages.APPENGINE_IMAGE_DESCRIPTOR);
      }
    } else if (element instanceof DatastoreIndexes) {
      return resources.createImage(SharedImages.DATASTORE_GREY_IMAGE_DESCRIPTOR);
    } else if (element instanceof AppEngineResourceElement) {
      // todo CronDescriptor should be a timer/clock
      // todo DenialOfServiceDescriptor should be a do-not-enter
      // todo RequestDispatchDescriptor should be a path fork
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
      resources = null;
    }
  }
}
