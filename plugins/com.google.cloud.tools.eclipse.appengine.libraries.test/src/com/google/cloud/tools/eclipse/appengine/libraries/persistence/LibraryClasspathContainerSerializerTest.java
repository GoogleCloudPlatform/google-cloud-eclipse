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

package com.google.cloud.tools.eclipse.appengine.libraries.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainer;
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer.ArtifactBaseLocationProvider;
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer.LibraryContainerStateLocationProvider;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LibraryClasspathContainerSerializerTest {

  private static final String CONTAINER_DESCRIPTION = "Test container description";
  private static final String CONTAINER_PATH = "container/path";

  private static final String SERIALIZED_CONTAINER =
      "{"
      + "  \"description\": \"Test container description\","
      + "  \"path\": \"container/path\","
      + "  \"entries\": ["
      + "    {"
      + "      \"accessRules\": ["
      + "        {"
      + "          \"ruleKind\": \"ACCESSIBLE\","
      + "          \"pattern\": \"/com/example/accessible\""
      + "        },"
      + "        {"
      + "          \"ruleKind\": \"FORBIDDEN\","
      + "          \"pattern\": \"/com/example/nonaccessible\""
      + "        }"
      + "      ],"
      + "      \"sourceAttachmentPath\": \"SRC/path/to/src\","
      + "      \"path\": \"path/to/jar\","
      + "      \"attributes\": ["
      + "        {"
      + "          \"name\": \"attrName\","
      + "          \"value\": \"attrValue\""
      + "        }"
      + "      ]"
      + "    }"
      + "  ]"
      + "}";

  @Mock private LibraryContainerStateLocationProvider stateLocationProvider;
  @Mock private ArtifactBaseLocationProvider binaryBaseLocationProvider;
  @Mock private ArtifactBaseLocationProvider sourceBaseLocationProvider;
  @Mock private IJavaProject javaProject;

  @Rule
  public TemporaryFolder stateFolder = new TemporaryFolder();

  private LibraryClasspathContainer container;

  @Before
  public void setUp() throws Exception {
    IClasspathEntry[] classpathEntries =
        new IClasspathEntry[] {getClasspathEntry(IClasspathEntry.CPE_LIBRARY, "/test/path/to/jar",
            "/test/path/to/src", new IClasspathAttribute[] {getAttribute("attrName", "attrValue")},
            new IAccessRule[] {getAccessRule("/com/example/accessible", true /* accessible */),
                getAccessRule("/com/example/nonaccessible", false /* accessible */)},
            true)};
    when(binaryBaseLocationProvider.getBaseLocation()).thenReturn(new Path("/test"));
    when(sourceBaseLocationProvider.getBaseLocation()).thenReturn(new Path("/test"));
    container = new LibraryClasspathContainer(new Path(CONTAINER_PATH), CONTAINER_DESCRIPTION,
        classpathEntries);
  }

  @Test
  public void testSaveAndLoadContainer() throws CoreException, IOException {
    Path stateFilePath = new Path(stateFolder.newFile().getAbsolutePath());
    when(stateLocationProvider.getContainerStateFile(any(IJavaProject.class), any(IPath.class),
        anyBoolean())).thenReturn(stateFilePath);
    LibraryClasspathContainerSerializer serializer = new LibraryClasspathContainerSerializer(
        stateLocationProvider, binaryBaseLocationProvider, sourceBaseLocationProvider);
    serializer.saveContainer(javaProject, container);
    LibraryClasspathContainer containerFromFile =
        serializer.loadContainer(javaProject, new Path(CONTAINER_PATH));
    compare(container, containerFromFile);
  }

  @Test
  public void testLoadContainer() throws IOException, CoreException {
    Path stateFilePath = new Path(stateFolder.newFile().getAbsolutePath());
    when(stateLocationProvider.getContainerStateFile(any(IJavaProject.class), any(IPath.class),
        anyBoolean())).thenReturn(stateFilePath);
    Files.write(stateFilePath.toFile().toPath(),
        SERIALIZED_CONTAINER.getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.TRUNCATE_EXISTING);
    LibraryClasspathContainerSerializer serializer = new LibraryClasspathContainerSerializer(
        stateLocationProvider, binaryBaseLocationProvider, sourceBaseLocationProvider);
    LibraryClasspathContainer containerFromFile =
        serializer.loadContainer(javaProject, new Path(CONTAINER_PATH));
    compare(container, containerFromFile);
  }

  @Test
  public void testSaveContainer() throws CoreException, IOException {
    Path stateFilePath = new Path(stateFolder.newFile().getAbsolutePath());
    when(stateLocationProvider.getContainerStateFile(any(IJavaProject.class), any(IPath.class),
        anyBoolean())).thenReturn(stateFilePath);
    LibraryClasspathContainerSerializer serializer = new LibraryClasspathContainerSerializer(
        stateLocationProvider, binaryBaseLocationProvider, sourceBaseLocationProvider);
    serializer.saveContainer(javaProject, container);
    byte[] data = Files.readAllBytes(stateFilePath.toFile().toPath());
    String actual = new String(data, StandardCharsets.UTF_8);
    // use JsonObject.equals()
    assertEquals(new JsonParser().parse(SERIALIZED_CONTAINER), new JsonParser().parse(actual));
  }

  @Test
  public void testSaveContainer_nullStateFileLocationNoError() throws IOException, CoreException {
    LibraryClasspathContainerSerializer serializer = new LibraryClasspathContainerSerializer(
        stateLocationProvider, binaryBaseLocationProvider, sourceBaseLocationProvider);
    serializer.saveContainer(javaProject, container);
  }

  @Test
  public void testLoadContainer_nullStateFileLocationNoError() throws IOException, CoreException {
    LibraryClasspathContainerSerializer serializer = new LibraryClasspathContainerSerializer(
        stateLocationProvider, binaryBaseLocationProvider, sourceBaseLocationProvider);
    assertNull(serializer.loadContainer(javaProject, new Path(CONTAINER_PATH)));
  }

  private void compare(LibraryClasspathContainer container,
      LibraryClasspathContainer otherContainer) {
    assertEquals(container.getPath(), otherContainer.getPath());
    assertEquals(container.getKind(), otherContainer.getKind());
    assertEquals(container.getDescription(), otherContainer.getDescription());
    for (int i = 0; i < container.getClasspathEntries().length; i++) {
      IClasspathEntry classpathEntry = container.getClasspathEntries()[i];
      IClasspathEntry otherClasspathEntry = otherContainer.getClasspathEntries()[i];
      assertEquals(classpathEntry.getPath(), otherClasspathEntry.getPath());
      assertEquals(classpathEntry.getEntryKind(), otherClasspathEntry.getEntryKind());
      assertEquals(classpathEntry.getSourceAttachmentPath(),
          otherClasspathEntry.getSourceAttachmentPath());
      assertEquals(classpathEntry.isExported(), otherClasspathEntry.isExported());
      for (int j = 0; j < classpathEntry.getAccessRules().length; j++) {
        IAccessRule accessRule = classpathEntry.getAccessRules()[j];
        IAccessRule otherAccessRule = otherClasspathEntry.getAccessRules()[j];
        assertEquals(accessRule.getKind(), otherAccessRule.getKind());
        assertEquals(accessRule.getPattern(), otherAccessRule.getPattern());
      }
      for (int k = 0; k < classpathEntry.getExtraAttributes().length; k++) {
        IClasspathAttribute classpathAttribute = classpathEntry.getExtraAttributes()[k];
        IClasspathAttribute otherClasspathAttribute = otherClasspathEntry.getExtraAttributes()[k];
        assertEquals(classpathAttribute.getName(), otherClasspathAttribute.getName());
        assertEquals(classpathAttribute.getValue(), otherClasspathAttribute.getValue());
      }
    }
  }

  private IClasspathEntry getClasspathEntry(final int entryKind, final String path,
      final String sourceAttachmentPath, final IClasspathAttribute[] attributes,
      final IAccessRule[] accessRules, final boolean isExported) {
    return new IClasspathEntry() {

      @Override
      public boolean isExported() {
        return isExported;
      }

      @Override
      public IPath getSourceAttachmentPath() {
        return new Path(sourceAttachmentPath);
      }

      @Override
      public IPath getPath() {
        return new Path(path);
      }

      @Override
      public IClasspathAttribute[] getExtraAttributes() {
        return attributes;
      }

      @Override
      public int getEntryKind() {
        return entryKind;
      }

      @Override
      public IAccessRule[] getAccessRules() {
        return accessRules;
      }

      // default implementation for the rest of the methods
      @Override
      public IPath getSourceAttachmentRootPath() {
        return null;
      }

      @Override
      public IClasspathEntry getResolvedEntry() {
        return null;
      }

      @Override
      public IClasspathEntry getReferencingEntry() {
        return null;
      }

      @Override
      public IPath getOutputLocation() {
        return null;
      }

      @Override
      public IPath[] getInclusionPatterns() {
        return null;
      }

      @Override
      public IPath[] getExclusionPatterns() {
        return null;
      }

      @Override
      public int getContentKind() {
        return 0;
      }

      @Override
      public boolean combineAccessRules() {
        return false;
      }
    };
  }

  private IClasspathAttribute getAttribute(final String name, final String value) {
    return new IClasspathAttribute() {

      @Override
      public String getValue() {
        return value;
      }

      @Override
      public String getName() {
        return name;
      }
    };
  }

  private IAccessRule getAccessRule(final String pattern, final boolean accessible) {
    return new IAccessRule() {

      @Override
      public boolean ignoreIfBetter() {
        return false;
      }

      @Override
      public IPath getPattern() {
        return new Path(pattern);
      }

      @Override
      public int getKind() {
        if (accessible) {
          return IAccessRule.K_ACCESSIBLE;
        } else {
          return IAccessRule.K_NON_ACCESSIBLE;
        }
      }
    };
  }
}
