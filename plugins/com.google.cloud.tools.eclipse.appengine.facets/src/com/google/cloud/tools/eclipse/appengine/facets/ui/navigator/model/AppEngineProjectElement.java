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
import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineConfigurationUtil;
import com.google.cloud.tools.project.AppYaml;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.StyledString;
import org.xml.sax.SAXException;

/**
 * A model representation of an App Engine project. App Engine projects always have a descriptor
 * ({@code app.yaml} or {@code appengine-web.xml}) that may provide their App Engine environment
 * type (standard or flexible), runtime type, and Service ID. This element manages model elements
 * representations of the various App Engine configuration files.
 */
public class AppEngineProjectElement implements IAdaptable {

  /** Special project file that describes the virtual folder layout used for building WARs. */
  private static final IPath WTP_COMPONENT_PATH =
      new Path(".settings/org.eclipse.wst.common.component"); // $NON-NLS-1$

  /** Special project file that records the project's facets. */
  private static final IPath WTP_FACETS_PATH =
      new Path(".settings/org.eclipse.wst.common.project.facet.core.xml"); // $NON-NLS-1$

  /** Special project file that records the project's facets. */
  private static final ImmutableSet<String> APPENGINE_DESCRIPTOR_FILENAMES =
      ImmutableSet.of("appengine-web.xml", "app.yaml"); //$NON-NLS-1$ //$NON-NLS-2$
  
  /** Factories to create model elements for the various App Engine configuration files. */
  private static final Map<String, Function<IFile, AppEngineResourceElement>> elementFactories =
      new ImmutableMap.Builder<String, Function<IFile, AppEngineResourceElement>>()
          .put("cron.yaml", file -> new CronDescriptor(file)) // $NON-NLS-1$
          .put("index.yaml", file -> new DatastoreIndexesDescriptor(file)) // $NON-NLS-1$
          .put("queue.yaml", file -> new TaskQueuesDescriptor(file)) // $NON-NLS-1$
          .put("dos.yaml", file -> new DenialOfServiceDescriptor(file)) // $NON-NLS-1$
          .put("dispatch.yaml", file -> new DispatchRoutingDescriptor(file)) // $NON-NLS-1$
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
  public static AppEngineProjectElement create(IProject project) throws AppEngineException {
    AppEngineProjectElement appEngineProject = new AppEngineProjectElement(project);
    appEngineProject.reload();
    return appEngineProject;
  }

  /**
   * Find the App Engine descriptor ({@code app.yaml} or {@code appengine-web.xml}) for this
   * project.
   *
   * @throws AppEngineException if the descriptor cannot be found
   */
  private static IFile findAppEngineDescriptor(IProject project) throws AppEngineException {
    // Which of app.yaml or appengine-web.xml should win?

    IFile descriptorFile =
        AppEngineConfigurationUtil.findConfigurationFile(project, new Path("appengine-web.xml"));
    if (descriptorFile != null && descriptorFile.exists()) {
      return descriptorFile;
    }

    descriptorFile =
        AppEngineConfigurationUtil.findConfigurationFile(project, new Path("app.yaml"));
    if (descriptorFile != null && descriptorFile.exists()) {
      return descriptorFile;
    }
    throw new AppEngineException("App Engine descriptor not found");
  }

  /** Return {@code true} if the changed files may result in a different virtual layout. */
  @VisibleForTesting
  static boolean hasLayoutChanged(Collection<IFile> changedFiles) {
    Preconditions.checkNotNull(changedFiles);
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

  /**
   * Return {@code true} if the list of changed files includes an App Engine descriptor. This file
   * may not necessarily be the resolved descriptor.
   */
  public static boolean hasAppEngineDescriptor(Collection<IFile> changedFiles) {
    Preconditions.checkNotNull(changedFiles);
    return Iterables.any(
        changedFiles,
        file -> file != null && APPENGINE_DESCRIPTOR_FILENAMES.contains(file.getName()));
  }

  private final IProject project;

  /**
   * The App Engine descriptor file; may change due to layout changes but should never be {@code
   * null}.
   */
  private IFile descriptorFile;

  private String projectId;
  private String projectVersion;
  private String serviceId;

  /**
   * The value of the App Engine environment type (i.e., {@code standard} or {@code flex}). This
   * usually corresponds to the {@code env:} value.
   */
  private String environmentType;

  /**
   * Return the App Engine runtime (e.g., java7, java8). This usually corresponds to the {@code
   * <runtime>} or {@code runtime:} value.
   */
  private String runtime;

  /** Map of <em>base-file-name &rarr; model-element</em> pairs. */
  private final Map<String, AppEngineResourceElement> configurations = new TreeMap<>();

  private AppEngineProjectElement(IProject project) throws AppEngineException {
    this.project = project;
    descriptorFile = findAppEngineDescriptor(project);
  }

  /** Return the project. */
  public IProject getProject() {
    return project;
  }

  /** Return the descriptor file. */
  public IFile getDescriptorFile() {
    return descriptorFile;
  }

  /** Adapts to {@link IFile} to allow double-clicking to open the corresponding file. */
  @Override
  public <T> T getAdapter(Class<T> adapter) {
    if (adapter.isInstance(descriptorFile)) {
      return adapter.cast(descriptorFile);
    }
    return null;
  }

  /** Return the configuration file models. Never {@code null}. */
  public AppEngineResourceElement[] getConfigurations() {
    return configurations.values().toArray(new AppEngineResourceElement[configurations.size()]);
  }

  /** Return the GCP Project ID, or {@code null} if not specified. */
  public String getProjectId() {
    return projectId;
  }

  /** Return the GCP Project Version, or {@code null} if not specified. */
  public String getProjectVersion() {
    return projectVersion;
  }

  /**
   * Return the App Engine Service ID, or {@code null} if not specified (which is {@code
   * "default"}).
   */
  public String getServiceId() {
    return serviceId;
  }

  /** Return the App Engine environment type: {@code standard} or {@code flex}. */
  @VisibleForTesting
  String getEnvironmentType() {
    return environmentType;
  }

  /**
   * Return the App Engine runtime type, which may depend on the {@link #getEnvironmentType()
   * environment type}.
   */
  @VisibleForTesting
  String getRuntime() {
    return runtime;
  }

  public StyledString getStyledLabel() {
    StyledString result = new StyledString("App Engine");
    result.append(" [", StyledString.QUALIFIER_STYLER);
    result.append(getEnvironmentType(), StyledString.QUALIFIER_STYLER);
    result.append(": ", StyledString.QUALIFIER_STYLER);
    result.append(getRuntime(), StyledString.QUALIFIER_STYLER);
    result.append("]", StyledString.QUALIFIER_STYLER);
    result.append(" - " + descriptorFile.getName(), StyledString.DECORATIONS_STYLER);
    return result;
  }

  /**
   * Update to the set of resource modifications in this project (added, removed, or changed).
   * Return {@code true} if there were changes.
   *
   * @throws AppEngineException when some error occurred parsing or interpreting some relevant file
   */
  public boolean resourcesChanged(Collection<IFile> changedFiles) throws AppEngineException {
    Preconditions.checkNotNull(changedFiles);
    Preconditions.checkNotNull(descriptorFile);

    boolean layoutChanged = hasLayoutChanged(changedFiles); // files may be newly exposed or removed
    boolean hasNewDescriptor =
        (layoutChanged || hasAppEngineDescriptor(changedFiles))
            && !descriptorFile.equals(findAppEngineDescriptor(project));

    if (changedFiles.contains(descriptorFile) || hasNewDescriptor) {
      // reload everything: e.g., may no longer be "default"
      reload();
      return true;
    } else if (!descriptorFile.exists()) {
      // if our descriptor was removed then we're not really an App Engine project
      throw new AppEngineException(descriptorFile.getName() + " no longer exists");
    }
    // So descriptor is unchanged.

    if (!isDefaultService()) {
      // Only the default service carries ancilliary configuration files
      Preconditions.checkState(configurations.isEmpty());
      return false;
    } else if (layoutChanged) {
      // Reload as new configuration files may have become available or previous
      // configuration files may have disappeared
      return reloadConfigurationFiles();
    }

    // Since this is called on any file change to the project (e.g., to a java or text file),
    // we walk the files and see if they may correspond to an App Engine configuration file to
    // avoid unnecessary work. Since the layout hasn't changed then (1) reload any changed
    // configuration file models, (2) remove any deleted models, and (3) add models for new files.
    boolean changed = false;
    for (IFile file : changedFiles) {
      String baseName = file.getName();
      AppEngineResourceElement previous = configurations.get(baseName);
      if (previous != null) {
        // Since first file resolved wins check if this file was (and thus remains) the winner
        if (file.equals(previous.getFile())) {
          // Case 1 and 2: reload() returns null if underlying file no longer exists
          configurations.compute(baseName, (ignored, element) -> element.reload());
          changed = true;
        }
      } else if (elementFactories.containsKey(baseName)) {
        // Case 3: file has a recognized configuration file name
        AppEngineResourceElement current = configurations.compute(baseName, this::updateElement);
        // updateElement() returns null if file not resolved
        changed |= current != null;
      }
    }
    return changed;
  }

  /** Return {@code true} if this is the default service. */
  private boolean isDefaultService() {
    return serviceId == null || "default".equals(serviceId);
  }

  /**
   * Reload all data.
   *
   * @throws AppEngineException if the descriptor or some other configuration file has errors
   */
  private void reload() throws AppEngineException {
    descriptorFile = findAppEngineDescriptor(project);
    try (InputStream input = descriptorFile.getContents()) {
      if ("app.yaml".equals(descriptorFile.getName())) {
        AppYaml descriptor = AppYaml.parse(input);
        projectId = descriptor.getProjectId();
        projectVersion = descriptor.getProjectVersion();
        serviceId = descriptor.getServiceId();
        // flex always has `env: flex`; we ignore Managed VM
        environmentType =
            Strings.isNullOrEmpty(descriptor.getEnvironmentType())
                ? "standard"
                : descriptor.getEnvironmentType();
        runtime = descriptor.getRuntime();
        if (Strings.isNullOrEmpty(runtime)) {
          throw new AppEngineException("missing runtime: element");
        }
      } else {
        AppEngineDescriptor descriptor = AppEngineDescriptor.parse(input);
        projectId = descriptor.getProjectId();
        projectVersion = descriptor.getProjectVersion();
        serviceId = descriptor.getServiceId();
        environmentType = "standard";
        runtime =
            Strings.isNullOrEmpty(descriptor.getRuntime()) ? "java7" : descriptor.getRuntime();
      }
    } catch (IOException | SAXException | CoreException ex) {
      projectId = null;
      projectVersion = null;
      serviceId = null;
      environmentType = null;
      runtime = null;
      configurations.clear();
      throw new AppEngineException(
          "Unable to load appengine descriptor from " + descriptorFile, ex);
    }
    reloadConfigurationFiles();
  }

  /**
   * Reload the ancillary configuration files. Returns {@code true} if there were changes.
   *
   * @throws AppEngineException if the descriptor has errors or could not be loaded
   */
  private boolean reloadConfigurationFiles() throws AppEngineException {
    // ancillary config files are only taken from the default module
    if (!isDefaultService()) {
      boolean wasEmpty = configurations.isEmpty();
      configurations.clear();
      return !wasEmpty;
    }

    boolean changed = false;
    // check and re-resolve all configuration files
    for (String baseName : elementFactories.keySet()) {
      AppEngineResourceElement previous = configurations.get(baseName);
      AppEngineResourceElement created = configurations.compute(baseName, this::updateElement);
      changed |= created != previous;
    }
    return changed;
  }

  /**
   * Update a possibly-existing configuration file element. Return the replacement element or {@code
   * null} if the configuration file no longer exists.
   */
  private AppEngineResourceElement updateElement(
      String baseName, AppEngineResourceElement element) {
    Preconditions.checkArgument(elementFactories.containsKey(baseName));
    // Check that each model element, if present, corresponds to the current configuration file.
    // Rebuild the element representation using the provided element creator, or remove
    // it, as required.
    IFile configurationFile =
        AppEngineConfigurationUtil.findConfigurationFile(project, new Path(baseName));
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
