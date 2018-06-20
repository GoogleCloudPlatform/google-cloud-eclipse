/*
 * Copyright 2018 Google LLC
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

package com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.StyledString;
import org.xml.sax.SAXException;

/**
 * A model representation of an App Engine project. App Engine projects always have a descriptor
 * (e.g., {@code appengine-web.xml}) that may provides their environment, runtime type, and Service
 * ID. This element manages model elements representations of the various App Engine configuration
 * files.
 */
public class AppEngineStandardProjectElement {

  /** Special project file that describes the virtual folder layout used for building WARs. */
  private static final IPath WTP_COMPONENT_PATH =
      new Path(".settings/org.eclipse.wst.common.component"); // $NON-NLS-1$

  /** Special project file that records the project's facets. */
  private static final IPath WTP_FACETS_PATH =
      new Path(".settings/org.eclipse.wst.common.project.facet.core.xml"); // $NON-NLS-1$

  private static final Map<String, Function<IFile, AppEngineResourceElement>> elementFactories =
      new ImmutableMap.Builder<String, Function<IFile, AppEngineResourceElement>>()
          .put("cron.xml", file -> new CronDescriptor(file)) // $NON-NLS-1$
          .put("datastore-indexes.xml", file -> new DatastoreIndexesDescriptor(file)) // $NON-NLS-1$
          .put("queue.xml", file -> new TaskQueuesDescriptor(file)) // $NON-NLS-1$
          .put("dos.xml", file -> new DenialOfServiceDescriptor(file)) // $NON-NLS-1$
          .put("dispatch.xml", file -> new DispatchRoutingDescriptor(file)) // $NON-NLS-1$
          .build();

  /**
   * Create and populate for the given project.
   *
   * @throws AppEngineException when unable to parse the descriptor file ({@code appengine-web.xml})
   */
  public static AppEngineStandardProjectElement create(IProject project) throws AppEngineException {
    AppEngineStandardProjectElement appEngineProject = new AppEngineStandardProjectElement(project);
    appEngineProject.reload();
    return appEngineProject;
  }

  /**
   * Find the App Engine descriptor ({@code appengine-web.xml}) for this project.
   *
   * @throws AppEngineException if the descriptor cannot be found
   */
  private static IFile findAppEngineDescriptor(IProject project) throws AppEngineException {
    IFile descriptorFile = WebProjectUtil.findInWebInf(project, new Path("appengine-web.xml"));
    if (descriptorFile == null || !descriptorFile.exists()) {
      throw new AppEngineException("appengine-web.xml not found");
    }
    return descriptorFile;
  }

  /** Return {@code true} if the changed files may result in a different virtual layout. */
  @VisibleForTesting
  static boolean hasLayoutChanged(Collection<IFile> changedFiles) {
    // the virtual layout may have been reconfigured, or no longer an App Engine project
    for (IFile changed : changedFiles) {
      IPath projectRelativePath = changed.getProjectRelativePath();
      if (WTP_COMPONENT_PATH.equals(projectRelativePath)
          || WTP_FACETS_PATH.equals(projectRelativePath)) {
        return true;
      }
    }
    return false;
  }

  private final IProject project;

  /** The App Engine descriptor file; may change. */
  private IFile descriptorFile;

  private AppEngineDescriptor descriptor;

  /**
   * Map of <em>base-file-name &rarr; model-element</em> pairs, sorted by the
   * <em>base-file-name</em> (e.g., <code>dispatch.xml</code>).
   */
  private final Map<String, AppEngineResourceElement> configurations = new TreeMap<>();

  private AppEngineStandardProjectElement(IProject project) throws AppEngineException {
    this.project = project;
    this.descriptorFile = findAppEngineDescriptor(project);
  }

  /** Return the project. */
  public IProject getProject() {
    return project;
  }

  /** Return the descriptor file. */
  public IFile getFile() {
    return descriptorFile;
  }

  /** Return the configuration file models. */
  public AppEngineResourceElement[] getConfigurations() {
    return configurations.values().toArray(new AppEngineResourceElement[configurations.size()]);
  }

  public String getRuntimeType() {
    try {
      String runtime = descriptor.getRuntime();
      return "standard: " + (Strings.isNullOrEmpty(runtime) ? "java7" : runtime);
    } catch (AppEngineException ex) {
      return null;
    }
  }

  public AppEngineDescriptor getDescriptor() {
    return descriptor;
  }

  public StyledString getStyledLabel() {
    StyledString result = new StyledString("App Engine");
    String qualifier = getRuntimeType();
    if (qualifier != null) {
      result.append(" [" + qualifier + "]", StyledString.QUALIFIER_STYLER);
    }
    return result;
  }

  /**
   * Handle a change to given resource (added, removed, or changed), and return the model objects
   * that were changed.
   *
   * @throws AppEngineException when some error occurred parsing or interpreting some relevant file
   */
  public Collection<Object> resourcesChanged(Collection<IFile> changedFiles)
      throws AppEngineException {
    Preconditions.checkNotNull(changedFiles);
    Preconditions.checkNotNull(descriptorFile);

    boolean layoutChanged = hasLayoutChanged(changedFiles);
    boolean hasNewDescriptor =
        layoutChanged && !descriptorFile.equals(findAppEngineDescriptor(project));
    // virtual layout may have changed or a new descriptor may have been added
    if (changedFiles.contains(descriptorFile) || hasNewDescriptor) {
      // reload everything: e.g., may no longer be "default"
      reload();
      return Collections.singleton(this);
    } else if (!descriptorFile.exists()) {
      // if our descriptor was removed then we're no longer an App Engine project
      throw new AppEngineException(descriptorFile.getName() + " no longer exists");
    } else if (layoutChanged) {
      // new config file may have become available
      return reloadConfigurationFiles();
    }

    // reload any existing configuration file models if the corresponding file has changed
    // but track if any previously-absent configuration files have been seen
    Set<Object> changed = new HashSet<>();
    for (IFile file : changedFiles) {
      String baseName = file.getName();
      AppEngineResourceElement element = configurations.compute(baseName, this::updateElement);
      // if null then was deleted, so we return this project element
      changed.add(element == null ? this : element);
    }
    return changed;
  }

  /**
   * Reload all data.
   *
   * @throws AppEngineException if the descriptor or some other configuration file has errors
   */
  private void reload() throws AppEngineException {
    descriptorFile = findAppEngineDescriptor(project);
    try (InputStream input = descriptorFile.getContents()) {
      descriptor = AppEngineDescriptor.parse(input);
    } catch (IOException | SAXException | CoreException ex) {
      throw new AppEngineException(
          "Unable to load appengine descriptor from " + descriptorFile, ex);
    }
    reloadConfigurationFiles();
  }

  /**
   * Reload the ancillary configuration files. Returns changed or new elements.
   *
   * @throws AppEngineException if the descriptor has errors or could not be loaded
   */
  private Collection<Object> reloadConfigurationFiles() throws AppEngineException {
    // ancillary config files are only taken from the default module
    if (descriptor.getServiceId() != null && !"default".equals(descriptor.getServiceId())) {
      configurations.clear();
      return Collections.singleton(this);
    }

    Set<Object> changed = new HashSet<>();
    // check for all configuration files
    for (String baseName : elementFactories.keySet()) {
      AppEngineResourceElement previous = configurations.get(baseName);
      AppEngineResourceElement created = configurations.compute(baseName, this::updateElement);
      if (created != previous) {
        changed.add(created);
      }
    }
    return changed;
  }

  private AppEngineResourceElement updateElement(
      String baseName, AppEngineResourceElement element) {
    Preconditions.checkArgument(elementFactories.containsKey(baseName));
    // Check that each model element, if present, corresponds to the current configuration file.
    // Rebuild the element representation using the provided element creator, or remove
    // it, as required.
    IFile configurationFile = WebProjectUtil.findInWebInf(project, new Path(baseName));
    if (configurationFile == null || !configurationFile.exists()) {
      // remove the element e.g., file has disappeared
      return null;
    } else if (element == null || !configurationFile.equals(element.getFile())) {
      // create or recreate the element
      return elementFactories.get(baseName).apply(configurationFile);
    } else {
      return element.reload();
    }
  }
}
