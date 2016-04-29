package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class CodeTemplatesTest {

  private IProgressMonitor monitor = new NullProgressMonitor();
  private IFolder parent;
  
  @Before
  public void setUp() throws CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject project = workspace.getRoot().getProject("foobar");
    if (!project.exists()) {
      project.create(monitor);
      project.open(monitor);
    }
    parent = project.getFolder("testfolder");
    if (!parent.exists()) {
      parent.create(true, true, monitor);
    }
  }
  
  @After
  public void deleteWorkspace() throws CoreException {
    // todo how?
  }
  
  @Test
  public void testCreateChildFolder() throws CoreException {
    IFolder child = CodeTemplates.createChildFolder("testchild", parent, monitor);
    Assert.assertTrue(child.exists());
    Assert.assertEquals("testchild", child.getName());
  }
  
  @Test
  public void testCreateChildFile() throws CoreException {
    IFile child = CodeTemplates.createChildFile("web.xml", parent, monitor);
    Assert.assertTrue(child.exists());
    Assert.assertEquals("web.xml", child.getName());
  }

}
