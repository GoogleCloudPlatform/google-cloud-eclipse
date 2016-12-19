package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainer;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.File;
import java.io.IOException;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class M2SourceAttachmentDownloaderJobTest {

  private static final String CONTAINER_PATH = "/containerPath";
  private static final String LIBRARY_PATH = "/libraryPath";

  @Rule
  public TestProjectCreator testProjectCreator = new TestProjectCreator().withClasspathContainerPath(CONTAINER_PATH);
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Mock
  private MavenHelper mavenHelper;
  @Mock
  private Artifact artifact;
  @Mock
  private LibraryClasspathContainerSerializer serializer;
  private IClasspathEntry library;
  private LibraryClasspathContainer container;

  @Before
  public void setUp() {
    library = JavaCore.newLibraryEntry(new Path(LIBRARY_PATH), null, null);
    container = new LibraryClasspathContainer(new Path(CONTAINER_PATH), "Test Container", new IClasspathEntry[]{ library });
  }
  
  @Test
  public void test() throws InterruptedException, CoreException, IOException {
    File sourceArtifact = temporaryFolder.newFile("testSourceArtifact");
    when(artifact.getFile()).thenReturn(sourceArtifact);
    when(mavenHelper.getMavenSourceJarLocation(any(MavenCoordinates.class))).thenReturn(new Path(sourceArtifact.getAbsolutePath()));
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    IJavaProject javaProject = testProjectCreator.getJavaProject();
    JavaCore.setClasspathContainer(new Path(CONTAINER_PATH), new IJavaProject[]{ javaProject }, new IClasspathContainer[]{ container }, null);
    M2SourceAttachmentDownloaderJob job = new M2SourceAttachmentDownloaderJob(javaProject,
                                                                              new Path(LIBRARY_PATH),
                                                                              mavenCoordinates,
                                                                              mavenHelper,
                                                                              serializer);
    job.schedule();
    job.join();
    assertThat(job.getResult(), is(Status.OK_STATUS));
    boolean libraryFound = false;
    for (IClasspathEntry iClasspathEntry : javaProject.getResolvedClasspath(false)) {
      if (iClasspathEntry.getPath().equals(new Path(LIBRARY_PATH))) {
        libraryFound = true;
        assertThat(iClasspathEntry.getSourceAttachmentPath().toFile().getAbsolutePath(), is(sourceArtifact.getAbsolutePath()));
      }
    }
    assertTrue(libraryFound);
  }

}
