package com.google.cloud.tools.eclipse.appengine.newproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class CodeTemplatesTest {

  private SubMonitor monitor = SubMonitor.convert(new NullProgressMonitor());
  private IFolder parent;
  private IProject project;
  
  @Before
  public void setUp() throws CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    project = workspace.getRoot().getProject("foobar");
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
  public void cleanUp() throws CoreException {
    parent.delete(true, monitor);
  }
  
  @Test
  public void testMaterialize() throws CoreException {
    AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();
    CodeTemplates.materialize(project, config, monitor);
    IFolder src = project.getFolder("src");
    IFolder main = src.getFolder("main");
    IFolder java = main.getFolder("java");
    IFile servlet = java.getFile("HelloAppEngine.java");
    Assert.assertTrue(servlet.exists());
  }
  
  @Test
  public void testCreateChildFolder() throws CoreException {
    IFolder child = CodeTemplates.createChildFolder("testchild", parent, monitor);
    Assert.assertTrue(child.exists());
    Assert.assertEquals("testchild", child.getName());
  }
  
  @Test
  public void testCreateChildFile() throws CoreException, IOException {
    IFile child = CodeTemplates.createChildFile("web.xml", parent, monitor);
    Assert.assertTrue(child.exists());
    Assert.assertEquals("web.xml", child.getName());
    InputStream in = child.getContents(true);
    Assert.assertNotEquals("File is empty", -1, in.read());
  }
  
  @Test
  public void testCreateChildFileWithTemplates() throws CoreException, IOException {
    Map<String, String> values = new HashMap<>();
    values.put("Package", "package com.google.foo.bar;");
    
    IFile child = CodeTemplates.createChildFile("HelloAppEngine.java", parent, monitor, values);
    Assert.assertTrue(child.exists());
    Assert.assertEquals("HelloAppEngine.java", child.getName());
    InputStream in = child.getContents(true);
    BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
    Assert.assertEquals("package com.google.foo.bar;", reader.readLine());
    Assert.assertEquals("", reader.readLine());
  }

}
