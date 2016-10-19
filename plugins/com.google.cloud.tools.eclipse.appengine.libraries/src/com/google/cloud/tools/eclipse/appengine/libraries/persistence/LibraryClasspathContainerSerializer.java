package com.google.cloud.tools.eclipse.appengine.libraries.persistence;

import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainer;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.framework.FrameworkUtil;

public class LibraryClasspathContainerSerializer {

  private static final Logger logger = Logger.getLogger(LibraryClasspathContainerSerializer.class.getName());

  public interface LibraryContainerStateLocationProvider {
    IPath getContainerStateFile(IJavaProject javaProject, IPath containerPath, boolean create) throws CoreException;
  }

  private LibraryContainerStateLocationProvider stateLocationProvider;
  
  public LibraryClasspathContainerSerializer() {
    this(new DefaultStateLocationProvider());
  }
  
  @VisibleForTesting
  public LibraryClasspathContainerSerializer(LibraryContainerStateLocationProvider stateLocationProvider) {
    this.stateLocationProvider = stateLocationProvider;
  }

  public void saveContainer(IJavaProject javaProject, LibraryClasspathContainer container) throws IOException,
                                                                                                  CoreException {
    File stateFile = getContainerStateFile(javaProject, container.getPath(), true);
    if (stateFile == null) {
      logger.warning("Container state file cannot be created, save failed");
      return;
    }
    try (PrintWriter outputStream = new PrintWriter(stateFile)) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      outputStream.print(gson.toJson(new SerializableLibraryClasspathContainer(container)));
    }
  }

  public LibraryClasspathContainer loadContainer(IJavaProject javaProject, IPath containerPath) throws IOException,
                                                                                                       CoreException {
    File stateFile = getContainerStateFile(javaProject, containerPath, false);
    if (stateFile == null || !stateFile.exists()) {
      return null;
    }
    try (FileReader fileReader = new FileReader(stateFile)) {
      Gson gson = new GsonBuilder().create();
      SerializableLibraryClasspathContainer fromJson =
          gson.fromJson(fileReader, SerializableLibraryClasspathContainer.class);
      return fromJson.toLibraryClasspathContainer();
    }
  }
  
  private File getContainerStateFile(IJavaProject javaProject, IPath containerPath, boolean create) 
                                                                                                throws CoreException {
    IPath containerStateFile = stateLocationProvider.getContainerStateFile(javaProject, containerPath, create);
    if (containerStateFile != null) {
      return containerStateFile.toFile();
    } else {
      return null;
    }
  }
  
  private static class DefaultStateLocationProvider implements LibraryContainerStateLocationProvider {

    @Override
    public IPath getContainerStateFile(IJavaProject javaProject, IPath containerPath, boolean create) 
                                                                                                throws CoreException {
      IFolder settingsFolder = javaProject.getProject().getFolder(".settings");
      IFolder folder = settingsFolder.getFolder(FrameworkUtil.getBundle(getClass()).getSymbolicName());
      if (!folder.exists() && create) {
        folder.create(true, true, null);
      }
      IFile containerFile = folder.getFile(containerPath.segment(1) + ".container");
      if (!containerFile.exists() && create) {
        containerFile.create(new ByteArrayInputStream(new byte[0]), true, null);
      }
      return containerFile.getLocation();
    }
    
  }
}
