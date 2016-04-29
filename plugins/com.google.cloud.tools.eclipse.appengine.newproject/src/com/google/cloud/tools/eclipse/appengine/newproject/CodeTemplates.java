package com.google.cloud.tools.eclipse.appengine.newproject;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class CodeTemplates {

  /**
   * Load the named template into the supplied Eclipse project.
   *  
   * @param project the Eclipse project to be filled with templated code
   * @param config replacement values
   * @param monitor progress monitor
   * @param name directory from which to load template
   * @throws CoreException 
   */
  // todo: config details are going to vary based on type of template; need a more generic
  // solution such as key-value map or a different design
  // todo what if project isn't empty?
  // todo is there anyway to make this less Eclipse dependent? e.g. use java.io
  // instead of IProject/IFile/IFolder
  public static void materialize(IProject project, AppEngineStandardProjectConfig config,
      IProgressMonitor monitor, String name) throws CoreException {
    
    String packageName = config.getPackageName();
    createCode(monitor, project, packageName);
  }

  // todo replace with something that simply copies from a file system while replacing tokens
  private static void createCode(IProgressMonitor monitor, IProject project, String packageName) 
      throws CoreException {
    boolean force = true;
    boolean local = true;
    IFolder src = project.getFolder("src");
    if (!src.exists()) {
      src.create(force, local, monitor);
    }
    IFolder main = createChildFolder("main", src, monitor);
    IFolder java = createChildFolder("java", main, monitor);
    IFolder test = createChildFolder("test", src, monitor);
    IFolder testJava = createChildFolder("java", test, monitor);

    if (packageName != null && !packageName.isEmpty()) {
      String[] packages = packageName.split("\\.");
      IFolder parent = java;
      for (int i = 0; i < packages.length; i++) {
        parent = createChildFolder(packages[i], parent, monitor);
      }
      // now set up the test directory
      parent = testJava;
      for (int i = 0; i < packages.length; i++) {
        parent = createChildFolder(packages[i], parent, monitor);
      }
    }
    
    IFolder webapp = createChildFolder("webapp", main, monitor);
    createChildFile("appengine-web.xml", webapp, monitor);
    createChildFile("web.xml", webapp, monitor);
    IFolder webinf = createChildFolder("WEB-INF", webapp, monitor);
    createChildFile("index.xhtml", webinf, monitor);
  }

  // visible for testing
  static IFolder createChildFolder(String name, IFolder parent, IProgressMonitor monitor) 
      throws CoreException {
    boolean force = true;
    boolean local = true;
    IFolder child = parent.getFolder(name);
    if (!child.exists()) {
      child.create(force, local, monitor);
    }
    return child;
  }
  
  // visible for testing
  static IFile createChildFile(String name, IFolder parent, IProgressMonitor monitor) 
      throws CoreException {
    boolean force = true;
    IFile child = parent.getFile(name);
    InputStream in = CreateAppEngineStandardWtpProject.class.getResourceAsStream(
        "com/google/cloud/tools/eclipse/appengine/newproject/templates/" + name + ".ftl");
    // todo template processing    
    if (!child.exists()) {
      child.create(in, force, monitor);
    }
    return child;
  }

}
