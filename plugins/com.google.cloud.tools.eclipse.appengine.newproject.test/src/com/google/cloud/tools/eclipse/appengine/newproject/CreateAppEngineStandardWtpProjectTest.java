package com.google.cloud.tools.eclipse.appengine.newproject;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;;

@RunWith(MockitoJUnitRunner.class)
public class CreateAppEngineStandardWtpProjectTest {

  @Mock private IAdaptable adaptable;

  private NullProgressMonitor monitor = new NullProgressMonitor();
  private IProject project;
  
  @Before
  public void setUp() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    project = workspace.getRoot().getProject("foobar");
  }
  
  @After
  public void cleanUp() throws CoreException {
    project.delete(true, monitor);
  }
  
  @Test
  public void testConstructor() {
    AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();
    config.setProject(project);
    new CreateAppEngineStandardWtpProject(config, adaptable);
  }
  
  @Test
  public void testSetProjectIdPreference() {
    AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();
    config.setAppEngineProjectId("MyProjectId");
    config.setProject(project);
    CreateAppEngineStandardWtpProject creator = new CreateAppEngineStandardWtpProject(config, adaptable);
    
    creator.setProjectIdPreference(project);
    
    IEclipsePreferences preferences = new ProjectScope(project)
        .getNode("com.google.cloud.tools.eclipse.appengine.deploy");
    Assert.assertEquals("MyProjectId", preferences.get("project.id", "fail"));
  }
  
  @Test
  public void testUnitTestCreated() throws InvocationTargetException, CoreException {
    AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();
    config.setProject(project);
    CreateAppEngineStandardWtpProject creator = new CreateAppEngineStandardWtpProject(config, adaptable);
    creator.execute(new NullProgressMonitor());
    assertJunitAndHamcrestAreOnClasspath();
  }

  private void assertJunitAndHamcrestAreOnClasspath() throws CoreException, JavaModelException {
    assertTrue(project.hasNature(JavaCore.NATURE_ID));
    IJavaProject javaProject = JavaCore.create(project);
    assertTrue(javaProject.findType("org.junit.Assert").exists());
    assertTrue(javaProject.findType("org.hamcrest.CoreMatchers").exists());
  }

  @Test
  public void testNullConfig() {
    try {
      new CreateAppEngineStandardWtpProject(null, adaptable);
      Assert.fail("allowed null config");
    } catch (NullPointerException ex) {
      // success
    }
  }

}
