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
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.StyledString;
import org.xml.sax.SAXException;

/** A model representation of the {@code appengine-web.xml}. */
public class AppEngineStandardProject extends AppEngineResourceElement {

  /**
   * Create and populate for the given project.
   *
   * @throws AppEngineException when unable to retrieve from the appengine-web.xml
   */
  public static AppEngineStandardProject create(IProject project) throws AppEngineException {
    AppEngineStandardProject appEngineProject = new AppEngineStandardProject(project);
    appEngineProject.reloadDescriptor();
    appEngineProject.reloadConfigurations();
    return appEngineProject;
  }

  private AppEngineDescriptor descriptor;
  private final List<AppEngineResourceElement> configurations = new ArrayList<>();

  private AppEngineStandardProject(IProject project) {
    super(project, WebProjectUtil.findInWebInf(project, new Path("appengine-web.xml")));
  }

  public AppEngineResourceElement[] getConfigurations() {
    return configurations.toArray(new AppEngineResourceElement[configurations.size()]);
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
      // if the appengine-web or WTP deployment assembly change, reload everything (may get entirely
      // different files)
      if ("appengine-web.xml".equals(file.getName())
          || "org.eclipse.wst.common.component".equals(file.getName())) {
        reloadDescriptor();
        reloadConfigurations();
        // need to update from the project on (may no longer be "default", forexample)
        return getProject();
      } else {
        reloadConfigurations();
        return this;
      }
    } catch(AppEngineException ex) {
      return getProject();
    }
  }

  /**
   * Reload the appengine-web.xml descriptor.
   *
   * @throws AppEngineException
   */
  private void reloadDescriptor() throws AppEngineException {
    Preconditions.checkState(getFile() != null && getFile().exists());
    try (InputStream input = getFile().getContents()) {
      descriptor = AppEngineDescriptor.parse(input);
    } catch (IOException | SAXException | CoreException ex) {
      throw new AppEngineException("Unable to load appengine descriptor from " + getFile(), ex);
    }
    // remove all configs whose files may have been moved or removed
    configurations.removeIf(
        element ->
            !element.getFile().exists()
                || !element
                    .getFile()
                    .equals(
                        WebProjectUtil.findInWebInf(
                            getProject(), new Path(element.getFile().getName()))));
  }
  
  /**
   * Reload the ancillary configuration files.
   *
   * @throws AppEngineException
   */
  private void reloadConfigurations() throws AppEngineException {
    // ancillary config files are only taken from the default module
    if (descriptor.getServiceId() != null && !"default".equals(descriptor.getServiceId())) {
      configurations.clear();
      return;
    }
    BiConsumer<String, Function<IFile, AppEngineResourceElement>> prober =
        (fileName, elementCreator) -> {
          IFile configurationFile = WebProjectUtil.findInWebInf(getProject(), new Path(fileName));
          if (configurationFile != null && configurationFile.exists()) {
            configurations.add(elementCreator.apply(configurationFile));
          }
        };
    prober.accept("cron.xml", cronXml -> new CronDescriptor(getProject(), cronXml)); // $NON-NLS-1$
    prober.accept(
        "datastore-indexes.xml", // $NON-NLS-1$
        cronXml -> new DatastoreIndexesDescriptor(getProject(), cronXml));
    prober.accept(
        "queue.xml", cronXml -> new TaskQueuesDescriptor(getProject(), cronXml)); // $NON-NLS-1$
    prober.accept(
        "dos.xml", cronXml -> new DenialOfServiceDescriptor(getProject(), cronXml)); // $NON-NLS-1$
    prober.accept(
        "dispatch.xml", // $NON-NLS-1$
        cronXml -> new DispatchRoutingDescriptor(getProject(), cronXml));
  }
}
