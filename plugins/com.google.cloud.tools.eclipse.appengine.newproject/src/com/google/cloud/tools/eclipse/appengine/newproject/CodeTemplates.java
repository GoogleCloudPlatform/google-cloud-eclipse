package com.google.cloud.tools.eclipse.appengine.newproject;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

public class CodeTemplates {

  /**
   * Load the named template into the supplied Eclipse project.
   *  
   * @param project the Eclipse project to be filled with templated code
   * @param config replacement values
   * @param monitor progress monitor
   * @param name directory from which to load template
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
    
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
    subMonitor.setTaskName("Generating code");
    boolean force = true;
    boolean local = true;
    IFolder src = project.getFolder("src");
    if (!src.exists()) {
      src.create(force, local, subMonitor);
    }
    IFolder main = createChildFolder("main", src, subMonitor);
    IFolder java = createChildFolder("java", main, subMonitor);
    IFolder test = createChildFolder("test", src, subMonitor);
    IFolder testJava = createChildFolder("java", test, subMonitor);

    if (packageName != null && !packageName.isEmpty()) {
      String[] packages = packageName.split("\\.");
      IFolder parent = java;
      for (int i = 0; i < packages.length; i++) {
        parent = createChildFolder(packages[i], parent, subMonitor);
      }
      
      // now set up the test directory
      parent = testJava;
      for (int i = 0; i < packages.length; i++) {
        parent = createChildFolder(packages[i], parent, subMonitor);
      }
    }
    
    IFolder webapp = createChildFolder("webapp", main, subMonitor);
    createChildFile("appengine-web.xml", webapp, subMonitor);
    createChildFile("web.xml", webapp, subMonitor);
    IFolder webinf = createChildFolder("WEB-INF", webapp, subMonitor);
    createChildFile("index.xhtml", webinf, subMonitor);
  }

  // visible for testing
  static IFolder createChildFolder(String name, IFolder parent, SubMonitor monitor) 
      throws CoreException {
    monitor.subTask("Creating folder " + name);
    monitor.newChild(10);

    boolean force = true;
    boolean local = true;
    IFolder child = parent.getFolder(name);
    if (!child.exists()) {
      child.create(force, local, monitor);
    }
    return child;
  }
  
  // visible for testing
  static IFile createChildFile(String name, IFolder parent, SubMonitor monitor) 
      throws CoreException {
     
    monitor.subTask("Creating file " + name);
    monitor.newChild(20);
    
    boolean force = true;
    IFile child = parent.getFile(name);
    InputStream in = CodeTemplates.class.getResourceAsStream("templates/" + name + ".ftl");
    
    if (in == null) {
      IStatus status = new Status(Status.ERROR, "todo plugin ID", 2, 
          "Could not load template for " + name, null);
      throw new CoreException(status);
    }
    
    // todo template processing    
    if (!child.exists()) {
      child.create(in, force, monitor);
    }
    return child;
  }

}
