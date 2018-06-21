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

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineResourceElement;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineStandardProjectElement;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualFile;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Provides a simple model for representing major configuration elements of an App Engine project
 * intended for use with the Eclipse Common Navigator framework, as used in the Project Explorer.
 *
 * <p>To avoid unnecessary refreshes, this content provider strives to return the same objects
 * between calls to {@link #getChildren(Object)}.
 *
 * <p>An App Engine service is defined by an {@code appengine-web.xml} / {@code app.yaml}. The
 * {@code default} service may also carry a number of ancilliary configuration files ({@code
 * cron.xml}, {@code datastore-indexes.xml}, {@code dispatch.xml}, {@code queue.xml}). These files
 * are found under the {@code WEB-INF} directory. A change to one of these files may require
 * reconfiguring the associated model. Changing the Service ID in the {@code appengine-web.xml} such
 * that a service is no longer the {@code default} service would require removing all traces of the
 * ancilliary configuration files.
 *
 * <p>WTP uses a virtual layout to map the project files and folders into a WAR layout (c.f., {@link
 * ComponentCore}, {@link IVirtualFolder}, {@link IVirtualFile}; referenced in the UI as a
 * Deployment Assembly). Multiple {@link IFolder project folders} can be mapped to a {@link
 * IVirtualFolder virtual folder}. The virtual layout could be reconfigured such that a different
 * {@code appengine-web.xml} file is used — or the {@code appengine-web.xml} may no longer appear in
 * {@code WEB-INF}!
 */
public class AppEngineContentProvider implements ITreeContentProvider {
  private static final Logger logger = Logger.getLogger(AppEngineContentProvider.class.getName());
  private static final Object[] EMPTY_ARRAY = new Object[0];

  /** Try to get a {@link IProject} from the given element, return {@code null} otherwise. */
  private static IProject getProject(Object inputElement) {
    if (inputElement instanceof IFacetedProject) {
      return ((IFacetedProject) inputElement).getProject();
    } else if (inputElement instanceof IProject) {
      return (IProject) inputElement;
    }
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

  /**
   * Load a representation of an App Engine project from the given project.
   *
   * @throws AppEngineException if not an App Engine project
   */
  @VisibleForTesting
  static AppEngineStandardProjectElement loadRepresentation(IProject project)
      throws AppEngineException {
    Preconditions.checkNotNull(project);
    if (project == null || !project.exists() || !isStandard(project)) {
      throw new AppEngineException("Not an App Engine project");
    }
    AppEngineStandardProjectElement appEngineProject =
        AppEngineStandardProjectElement.create(project);
    return appEngineProject;
  }

  private final LoadingCache<IProject, AppEngineStandardProjectElement> projectMapping =
      CacheBuilder.newBuilder()
          .weakKeys()
          .build(
              new CacheLoader<IProject, AppEngineStandardProjectElement>() {
                @Override
                public AppEngineStandardProjectElement load(IProject key) throws Exception {
                  return AppEngineContentProvider.loadRepresentation(key);
                }
              });
  private IWorkspace workspace = ResourcesPlugin.getWorkspace();
  private StructuredViewer viewer;
  private BiConsumer<Collection<Object>, Collection<Object>> refreshHandler = this::refreshElements;
  private IResourceChangeListener resourceListener;

  public AppEngineContentProvider() {}

  @VisibleForTesting
  AppEngineContentProvider(BiConsumer<Collection<Object>, Collection<Object>> refreshHandler) {
    this.refreshHandler = refreshHandler;
  }

  @Override
  public void inputChanged(Viewer theViewer, Object oldInput, Object newInput) {
    this.viewer = (StructuredViewer) theViewer;
    if (resourceListener == null) {
      resourceListener = this::resourceChanged;
      workspace.addResourceChangeListener(resourceListener);
    }
  }

  /**
   * One or more resources changed in the workspace. See if we need to invalidate and/or refresh any
   * model elements, and then request that they be updated in the UI. <b>Note:</b> calls may come on
   * any thread.
   */
  private void resourceChanged(IResourceChangeEvent event) {
    Multimap<IProject, IFile> affected;
    try {
      affected = ResourceUtils.getAffectedFiles(event.getDelta());
    } catch (CoreException ex) {
      logger.log(Level.WARNING, "Could not determine affected files from resource delta", ex);
      return;
    }
    
    // Track elements requiring a refresh (which refreshes all children) vs update (just the
    // element's label) as refreshing a big project tree could be painful
    Set<Object> toBeRefreshed = new HashSet<>();
    Set<Object> toBeUpdated = new HashSet<>();
    
    for (IProject project : affected.keySet()) {
      if (!project.exists()) {
        projectMapping.invalidate(project);
        continue; // the explorer will update itself to remove the project
      }
      Collection<IFile> projectFiles = affected.get(project);
      // check if we've already created a model for this project
      AppEngineStandardProjectElement projectElement = projectMapping.getIfPresent(project);
      if (projectElement != null) {
        try {
          if (projectElement.resourcesChanged(projectFiles)) {
            // something changed in the App Engine content block
            toBeRefreshed.add(projectElement);
          }
          // Check for changes to our App Engine descriptor: the descriptor is used in the
          // project labels and so the label may need changing
          if (projectFiles.contains(projectElement.getFile())) {
            toBeUpdated.add(project);
          }
        } catch (AppEngineException ex) {
          // model is not valid given this change (e.g., perhaps the appengine-web.xml
          // has been removed or disappeared due to virtual layout change)
          projectMapping.invalidate(project);
          toBeRefreshed.add(project);
        }
      } else if (Iterables.any(
          projectFiles, file -> file != null && "appengine-web.xml".equals(file.getName()))) {
        // If we reached here, the project is unknown.  So trigger refresh of project since
        // descriptor file appears to have been newly introduced
        toBeRefreshed.add(project);
      }
    }
    if (!toBeRefreshed.isEmpty() || !toBeUpdated.isEmpty()) {
      refreshHandler.accept(toBeRefreshed, toBeUpdated);
    }
  }

  private void refreshElements(Collection<Object> toBeRefreshed, Collection<Object> toBeUpdated) {
    Control control = viewer.getControl();
    if (control == null || control.isDisposed()) {
      return;
    }
    control
        .getDisplay()
        .asyncExec(
            () -> {
              if (!control.isDisposed()) {
                toBeRefreshed.forEach(handle -> viewer.refresh(handle));
                toBeUpdated.forEach(handle -> viewer.update(handle, null));
              }
            });
  }

  @Override
  public Object[] getElements(Object inputElement) {
    return getChildren(inputElement);
  }

  @Override
  public boolean hasChildren(Object element) {
    if (element instanceof AppEngineStandardProjectElement) {
      AppEngineStandardProjectElement projectElement = (AppEngineStandardProjectElement) element;
      return projectElement.getConfigurations() != null
          && projectElement.getConfigurations().length > 0;
    }
    IProject project = getProject(element);
    if (project == null) {
      return false;
    }
    AppEngineStandardProjectElement webProject = projectMapping.getIfPresent(project);
    return webProject != null ? webProject.getConfigurations().length > 0 : true;
  }

  @Override
  public Object[] getChildren(Object parentElement) {
    if (parentElement instanceof AppEngineStandardProjectElement) {
      return ((AppEngineStandardProjectElement) parentElement).getConfigurations();
    }
    IProject project = getProject(parentElement);
    if (project != null) {
      try {
        AppEngineStandardProjectElement projectElement = projectMapping.get(project);
        return projectElement == null ? EMPTY_ARRAY : new Object[] {projectElement};
      } catch (ExecutionException ex) {
        // ignore: either not an App Engine project or some validation problem in
        // the appengine-web.xml that will be reported via Problems view
      }
    }
    return EMPTY_ARRAY;
  }

  @Override
  public Object getParent(Object element) {
    if (element instanceof AppEngineStandardProjectElement) {
      return ((AppEngineStandardProjectElement) element).getProject();
    } else if (element instanceof AppEngineResourceElement) {
      IProject project = ((AppEngineResourceElement) element).getProject();
      return projectMapping.getIfPresent(project);
    }
    return null;
  }

  @Override
  public void dispose() {
    if (resourceListener != null) {
      workspace.removeResourceChangeListener(resourceListener);
    }
  }
}
