/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.newproject;

import com.google.cloud.tools.eclipse.util.templates.appengine.AppEngineTemplateUtility;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;

public class CodeTemplates {

  /**
   * Creates files for a sample App Engine Standard project in the supplied Eclipse project.
   *
   * @param project the Eclipse project to be filled with templated code
   * @param config replacement values
   * @param monitor progress monitor
   * @return the most important file created that should be opened in an editor
   */
  public static IFile materializeAppEngineStandardFiles(IProject project,
      AppEngineProjectConfig config, IProgressMonitor monitor) throws CoreException {
    return materialize(project, config, true /* isStandardProject */, monitor);
  }

  /**
   * Creates files for a sample App Engine Flexible project in the supplied Eclipse project.
   *
   * @param project the Eclipse project to be filled with templated code
   * @param config replacement values
   * @param monitor progress monitor
   * @return the most important file created that should be opened in an editor
   */
  public static IFile materializeAppEngineFlexFiles(IProject project, AppEngineProjectConfig config,
      IProgressMonitor monitor) throws CoreException {
    return materialize(project, config, false /* isStandardProject */, monitor);
  }

  /**
   * Creates files for a sample App Engine project in the supplied Eclipse project.
   *
   * @param project the Eclipse project to be filled with templated code
   * @param config replacement values
   * @param isStandardProject true if project should be configured to have the App Engine Standard
   *   configuration files and false if project should have the App Engine Flexible configuration
   *   files.
   * @param monitor progress monitor
   * @return the most important file created that should be opened in an editor
   */
  private static IFile materialize(IProject project, AppEngineProjectConfig config,
      boolean isStandardProject, IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
    subMonitor.setTaskName("Generating code");
    boolean force = true;
    boolean local = true;
    IFolder src = project.getFolder("src");  //$NON-NLS-1$
    if (!src.exists()) {
      src.create(force, local, subMonitor.newChild(5));
    }

    IFolder main = createChildFolder("main", src, subMonitor.newChild(5)); //$NON-NLS-1$
    IFolder java = createChildFolder("java", main, subMonitor.newChild(5)); //$NON-NLS-1$
    IFolder test = createChildFolder("test", src, subMonitor.newChild(5)); //$NON-NLS-1$
    IFolder testJava = createChildFolder("java", test, subMonitor.newChild(5)); //$NON-NLS-1$

    String packageName = config.getPackageName();

    Map<String, String> templateValues = new HashMap<>();
    if (packageName != null && !packageName.isEmpty()) {
      templateValues.put("package", packageName);  //$NON-NLS-1$
    } else {
      templateValues.put("package", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    IFolder packageFolder = createFoldersForPackage(java, packageName, subMonitor.newChild(5));
    IFile hello = createChildFile("HelloAppEngine.java", //$NON-NLS-1$
        AppEngineTemplateUtility.HELLO_APPENGINE_TEMPLATE,
        packageFolder, templateValues, subMonitor.newChild(5));

    // now set up the test directory
    IFolder testPackageFolder =
        createFoldersForPackage(testJava, packageName, subMonitor.newChild(5));
    createChildFile("HelloAppEngineTest.java", //$NON-NLS-1$
        AppEngineTemplateUtility.HELLO_APPENGINE_TEST_TEMPLATE, testPackageFolder,
        templateValues, subMonitor.newChild(5));
    String mockHttpServletResponseTemplate = isStandardProject
        ? AppEngineTemplateUtility.MOCK_HTTPSERVLETRESPONSE_TEMPLATE
        : AppEngineTemplateUtility.MOCK_HTTPSERVLET31RESPONSE_TEMPLATE;
    createChildFile("MockHttpServletResponse.java", //$NON-NLS-1$
        mockHttpServletResponseTemplate, testPackageFolder, templateValues, subMonitor.newChild(5));

    IFolder webapp = createChildFolder("webapp", main, subMonitor.newChild(5)); //$NON-NLS-1$
    IFolder webinf = createChildFolder("WEB-INF", webapp, subMonitor.newChild(5)); //$NON-NLS-1$

    Map<String, String> properties = new HashMap<>();
    String service = config.getServiceName();
    if (!Strings.isNullOrEmpty(service)) {
      properties.put("service", service);  //$NON-NLS-1$
    }

    if (isStandardProject) {
      createChildFile("appengine-web.xml",  //$NON-NLS-1$
          AppEngineTemplateUtility.APPENGINE_WEB_XML_TEMPLATE,
          webinf, properties, subMonitor.newChild(5));
    } else {
      IFolder appengine = createChildFolder("appengine", main, subMonitor.newChild(5)); //$NON-NLS-1$
      createChildFile("app.yaml", AppEngineTemplateUtility.APP_YAML_TEMPLATE, //$NON-NLS-1$
          appengine, properties, subMonitor.newChild(5));
    }

    Map<String, String> packageMap = new HashMap<>();
    String packageValue = config.getPackageName().isEmpty() ? "" : config.getPackageName() + "."; //$NON-NLS-1$ //$NON-NLS-2$
    packageMap.put("package", packageValue);  //$NON-NLS-1$
    createChildFile("web.xml", AppEngineTemplateUtility.WEB_XML_TEMPLATE, webinf,  //$NON-NLS-1$
        packageMap, subMonitor.newChild(5));

    createChildFile("index.html", AppEngineTemplateUtility.INDEX_HTML_TEMPLATE, webapp, //$NON-NLS-1$
        Collections.<String, String>emptyMap(), subMonitor.newChild(5));

    copyChildFile("favicon.ico", webapp, subMonitor.newChild(5)); //$NON-NLS-1$

    return hello;
  }

  private static IFolder createFoldersForPackage(IFolder parentFolder,
                                                 String packageName,
                                                 SubMonitor subMonitor) throws CoreException {
    IFolder folder = parentFolder;
    if (packageName != null && !packageName.isEmpty()) {
      String[] packages = packageName.split("\\.");  //$NON-NLS-1$
      subMonitor.setWorkRemaining(packages.length);
      for (int i = 0; i < packages.length; i++) {
        folder = createChildFolder(packages[i], folder, subMonitor.newChild(1));
      }
    }
    return folder;
  }

  @VisibleForTesting
  static IFolder createChildFolder(String name, IFolder parent, SubMonitor monitor)
      throws CoreException {
    monitor.subTask("Creating folder " + name);

    boolean force = true;
    boolean local = true;
    IFolder child = parent.getFolder(name);
    if (!child.exists()) {
      child.create(force, local, monitor);
    }
    return child;
  }

  @VisibleForTesting
  static IFile createChildFile(String name, String template, IContainer parent,
      Map<String, String> values, SubMonitor monitor) throws CoreException {

    monitor.subTask("Creating file " + name);

    IFile child = parent.getFile(new Path(name));
    if (!child.exists()) {
      child.create(new ByteArrayInputStream(new byte[0]), true /* force */, monitor);
      AppEngineTemplateUtility.createFileContent(
          child.getLocation().toString(), template, values);
      child.refreshLocal(IResource.DEPTH_ZERO, monitor);
    }
    return child;
  }

  @VisibleForTesting
  static void copyChildFile(String name, IContainer parent, SubMonitor monitor)
      throws CoreException {
    monitor.subTask("Copying file " + name);

    IFile child = parent.getFile(new Path(name));
    if (!child.exists()) {
      AppEngineTemplateUtility.copyFileContent(child.getLocation().toString(), name);
      child.refreshLocal(IResource.DEPTH_ZERO, monitor);
    }
  }

}
