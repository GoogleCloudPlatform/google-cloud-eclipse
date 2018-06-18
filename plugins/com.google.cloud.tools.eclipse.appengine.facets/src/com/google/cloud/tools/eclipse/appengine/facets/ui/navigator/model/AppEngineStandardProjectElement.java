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
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
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
 * A model representation of the {@code appengine-web.xml}. This element manages model elements that
 * correspond to the various App Engine configuration files.
 */
public class AppEngineStandardProjectElement extends AppEngineResourceElement {

  /** Special project file that describes the virtual folder layout used for building WARs. */
  private static final IPath WTP_COMPONENT_PATH =
      new Path(".settings/org.eclipse.wst.common.component"); // $NON-NLS-1$

  /** Special project file that records the project's facets. */
  private static final IPath WTP_FACETS_PATH =
      new Path(".settings/org.eclipse.wst.common.project.facet.core.xml"); // $NON-NLS-1$
  
  private static final Set<String> CONFIGURATION_FILE_NAMES = 
      ImmutableSet.of("cron.xml", "dispatch.xml", "dos.xml", 
          "datastore-indexes.xml", "queue.xml"); 

  /**
   * Create and populate for the given project.
   *
   * @throws AppEngineException when unable to retrieve from the appengine-web.xml
   */
  public static AppEngineStandardProjectElement create(IProject project) throws AppEngineException {
    IFile descriptorFile = findAppEngineDescriptor(project);
    AppEngineStandardProjectElement appEngineProject =
        new AppEngineStandardProjectElement(project, descriptorFile);
    appEngineProject.reloadDescriptor();
    return appEngineProject;
  }

  /**
   * Find the App Engine descriptor ({@code appengine-web.xml}) for this project.
   *
   * @throws AppEngineException if not found
   */
  private static IFile findAppEngineDescriptor(IProject project) throws AppEngineException {
    IFile descriptorFile = WebProjectUtil.findInWebInf(project, new Path("appengine-web.xml"));
    if (!descriptorFile.exists()) {
      throw new AppEngineException("appengine-web.xml not found");
    }
    return descriptorFile;
  }

  private AppEngineDescriptor descriptor;
  
  /**
   * Map of <em>base-file-name &rarr; model-element</em> pairs, sorted by the
   * <em>base-file-name</em> (e.g., <code>dispatch.xml</code>).
   */
  private final Map<String, AppEngineResourceElement> configurations = new TreeMap<>();

  private AppEngineStandardProjectElement(IProject project, IFile descriptorFile) {
    super(project, descriptorFile);
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

  @Override
  public StyledString getStyledLabel() {
    StyledString result = new StyledString("App Engine");
    String qualifier = getRuntimeType();
    if (qualifier != null) {
      result.append(" [" + qualifier + "]", StyledString.QUALIFIER_STYLER);
    }
    return result;
  }

  /**
   * Check if this element is still valid given changes in the provided files.
   */
  public boolean isValid(Collection<IFile> changedFiles) {
    // if our descriptor (appengine-web.xml) was removed then not valid
    if (!getFile().exists()) {
      return false;
    }
    // virtual layout may have changed or a new descriptor may have been added
    boolean potentialNewDescriptor = hasPotentialNewDescriptor(changedFiles);
    if (potentialNewDescriptor) {
      try {
        IFile newDescriptorFile = findAppEngineDescriptor(getProject());
        return getFile().equals(newDescriptorFile);
      } catch (AppEngineException ex) {
        // a descriptor should have been found!
        return false;
      }
    }
    return true;
  }

  /**
   * Return {@code true} if the changed files may result in a different App Engine descriptor being
   * found.
   */
  private boolean hasPotentialNewDescriptor(Collection<IFile> changedFiles) {
    // a new descriptor may be overlayed on top of our old one
    for (IFile changed : changedFiles) {
      if ("appengine-web.xml".equals(changed.getName()) && !changed.equals(getFile())) {
        return true;
      }
    }
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
   * Handle a change to given resource (added, removed, or changed), and return the model objects
   * that were changed.
   */
  public Collection<Object> resourcesChanged(Collection<IFile> changedFiles) {
    // AppEngineContentProvider should discard this model when its descriptor is removed or if the
    // file has been supplanted (e.g., virtual overlay model finds a different appengine-web.xml)
    Preconditions.checkState(getFile().exists());
    Preconditions.checkNotNull(changedFiles);

    try {
      if (changedFiles.contains(getFile())) {
        // When the appengine-web changes, reload everything: e.g., may no longer be "default"
        reloadDescriptor(); // also reloads configuration file models
        return Collections.singleton(getProject());
      }
      // first reload any existing configuration file models if the corresponding file has changed
      // but track if any previously-absent configuration files have been seen
      boolean otherConfigurationFilesSeen = false;
      Set<Object> changed = new HashSet<>();
      for(IFile file : changedFiles) {
        String baseName = file.getName();
        if (configurations.containsKey(baseName)) {
          // seen before: allow the element to possibly replace itself
          AppEngineResourceElement updatedElement =
              configurations.computeIfPresent(baseName, (ignored, element) -> element.reload());
          changed.add(updatedElement);
        } else if (CONFIGURATION_FILE_NAMES.contains(baseName)
            || WTP_COMPONENT_PATH.lastSegment().equals(baseName)
            || WTP_FACETS_PATH.lastSegment().equals(baseName)) {
          otherConfigurationFilesSeen = true;
        }
      }
      if (otherConfigurationFilesSeen) {
        // pick up any new files
        reloadConfigurationFiles();
        return Collections.singleton(this);
      }
      return changed.contains(this) ? Collections.singleton(this) : changed;
    } catch (AppEngineException ex) {
      // Problem loading the appengine-web.xml file, likely because was saved in between edits.
      // Assume these validation problems are reported through the editor.
      return null;
    }
  }

  /**
   * Reload the appengine-web.xml descriptor.
   *
   * @throws AppEngineException if the descriptor has errors or could not be loaded
   */
  private void reloadDescriptor() throws AppEngineException {
    Preconditions.checkState(getFile() != null && getFile().exists());
    try (InputStream input = getFile().getContents()) {
      descriptor = AppEngineDescriptor.parse(input);
    } catch (IOException | SAXException | CoreException ex) {
      throw new AppEngineException("Unable to load appengine descriptor from " + getFile(), ex);
    }
    reloadConfigurationFiles();
  }

  /**
   * Reload the ancillary configuration files.
   *
   * @throws AppEngineException if the descriptor has errors or could not be loaded
   */
  private void reloadConfigurationFiles() throws AppEngineException {
    // ancillary config files are only taken from the default module
    if (descriptor.getServiceId() != null && !"default".equals(descriptor.getServiceId())) {
      configurations.clear();
      return;
    }

    checkConfigurationFile(
        "cron.xml", resolvedFile -> new CronDescriptor(getProject(), resolvedFile)); // $NON-NLS-1$
    checkConfigurationFile(
        "datastore-indexes.xml", // $NON-NLS-1$
        resolvedFile -> new DatastoreIndexesDescriptor(getProject(), resolvedFile));
    checkConfigurationFile(
        "queue.xml",
        resolvedFile -> new TaskQueuesDescriptor(getProject(), resolvedFile)); // $NON-NLS-1$
    checkConfigurationFile(
        "dos.xml",
        resolvedFile -> new DenialOfServiceDescriptor(getProject(), resolvedFile)); // $NON-NLS-1$
    checkConfigurationFile(
        "dispatch.xml", // $NON-NLS-1$
        resolvedFile -> new DispatchRoutingDescriptor(getProject(), resolvedFile));
  }

  /**
   * Check that the current element representation corresponds to the current configuration file.
   * Rebuild the element representation using the provided element creator, or remove it, as
   * required.
   *
   * @param fileName the name of the configuration file, expected under {@code WEB-INF}
   * @param elementFactory creates a new element from a configuration file
   */
  private void checkConfigurationFile(
      String fileName, Function<IFile, AppEngineResourceElement> elementFactory) {
    configurations.compute(
        fileName,
        (ignored, element) -> {
          IFile configurationFile = WebProjectUtil.findInWebInf(getProject(), new Path(fileName));
          if (configurationFile == null || !configurationFile.exists()) {
            // remove the element e.g., file has disappeared
            return null;
          } else if (element == null || !configurationFile.equals(element.getFile())) {
            // create or recreate the element
            return elementFactory.apply(configurationFile);
          }
          return element;
        });
  }
}
