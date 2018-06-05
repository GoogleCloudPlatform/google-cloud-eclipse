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
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.StyledString;
import org.xml.sax.SAXException;

/** A model representation of the {@code appengine-web.xml}. */
public class AppEngineStandardProjectElement extends AppEngineResourceElement {

  /**
   * Create and populate for the given project.
   *
   * @throws AppEngineException when unable to retrieve from the appengine-web.xml
   */
  public static AppEngineStandardProjectElement create(IProject project) throws AppEngineException {
    AppEngineStandardProjectElement appEngineProject = new AppEngineStandardProjectElement(project);
    appEngineProject.reloadDescriptor();
    appEngineProject.reloadConfigurationFiles();
    return appEngineProject;
  }

  private AppEngineDescriptor descriptor;
  /**
   * Map of <em>base-file-name &rarr; model-element</em> pairs, sorted by the
   * <em>base-file-name</em> (e.g., <code>dispatch.xml</code>).
   */
  private final Map<String, AppEngineResourceElement> configurations = new TreeMap<>();

  private AppEngineStandardProjectElement(IProject project) {
    super(project, WebProjectUtil.findInWebInf(project, new Path("appengine-web.xml")));
  }

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
   * Handle a change to given resource (added, removed, or changed), and return the model object to
   * be refreshed.
   */
  public Object resourceChanged(IFile file) {
    Preconditions.checkNotNull(file);
    Preconditions.checkArgument(file.getProject() == getProject());
    try {
      String baseName = file.getName();
      if ("appengine-web.xml".equals(baseName)
          || "org.eclipse.wst.common.component".equals(baseName)) {
        // if the appengine-web or WTP deployment assembly change, reload everything:
        // e.g., may no longer be "default", or deployment assembly changed so may
        // may get entirely different files)
        reloadDescriptor();
        reloadConfigurationFiles();
        return getProject();
      } else if (configurations.containsKey(baseName)) {
        // seen before: allow the element to possibly replace itself
        AppEngineResourceElement oldElement = configurations.get(baseName);
        AppEngineResourceElement newElement =
            configurations.computeIfPresent(baseName, (ignored, element) -> element.reload());
        return oldElement == newElement ? oldElement : this;
      } else {
        // check if a new configuration file
        reloadConfigurationFiles();
        return this;
      }
    } catch (AppEngineException ex) {
      // problem loading the appengine-web.xml file
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
