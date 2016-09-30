package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryBuilder;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineLibraryContainerInitializerTest {

  private final class TestProject extends ExternalResource {

    private IJavaProject javaProject;

    @Override
    protected void before() throws Throwable {
      createJavaProject("test" + Math.random());
    }

    @Override
    protected void after() {
      try {
        javaProject.getProject().delete(true, null);
      } catch (CoreException e) {
        fail("Could not delete project");
      }
    }

    public IJavaProject getJavaProject() {
      return javaProject;
    }

    private void createJavaProject(String projectName) throws CoreException, JavaModelException {
      IProjectDescription newProjectDescription = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
      newProjectDescription.setNatureIds(new String[]{JavaCore.NATURE_ID});
      IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      project.create(newProjectDescription, null);
      project.open(null);
      javaProject = JavaCore.create(project);
      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      IClasspathEntry[] newRawClasspath = new IClasspathEntry[rawClasspath.length + 1];
      System.arraycopy(rawClasspath, 0, newRawClasspath, 0, rawClasspath.length);
      newRawClasspath[newRawClasspath.length - 1] = JavaCore.newContainerEntry(new Path("aaa/bbb"));
      javaProject.setRawClasspath(newRawClasspath, null);
    }
  }

  @Mock private LibraryBuilder libraryBuilder;
  @Mock private IConfigurationElement configurationElement;

  @Rule
  public TestProject testProject = new TestProject();

  @Test
  public void test() throws CoreException {
    Library library = new Library("bbb");
    library.setLibraryFiles(Collections.singletonList(new LibraryFile(new MavenCoordinates("a", "b", "c"))));
    when(libraryBuilder.build(any(IConfigurationElement.class))).thenReturn(library);

    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement }, libraryBuilder);
    containerInitializer.initialize(new Path("aaa/bbb"), testProject.getJavaProject());

    IClasspathEntry[] resolvedClasspath = testProject.getJavaProject().getResolvedClasspath(false);
    assertThat(resolvedClasspath.length, is(2));
    IClasspathEntry libJar = resolvedClasspath[1];
    assertTrue(libJar.getPath().toOSString().endsWith("c.jar"));
    assertTrue(libJar.getSourceAttachmentPath().toOSString().endsWith("c.jar"));
  }
}
