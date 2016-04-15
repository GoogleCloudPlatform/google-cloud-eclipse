/*******************************************************************************
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.localserver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.internal.FacetedProjectNature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit test for {@link CloudSdkUtils}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CloudSdkUtilsTest {
  @Mock private Build mockBuild;
  @Mock private Model mockModel;
  @Mock private IProject mockProject;
  @Mock private IFile mockFile;

  /**
   * Tests that {@link CloudSdkUtils#hasGcloudMavenPlugin(Model)} returns
   * false when {@code Model} does not have the gcloud-maven-plugin.
   */
  @Test
  public void testHasGcloudMavenPlugin_noGcloudPlugin() {
    List<Plugin> plugins = createPluginList("myArtifactId1", "myGroupId1", "myArtifactId2",
        "myGroupId2");
    when(mockBuild.getPlugins()).thenReturn(plugins);
    when(mockModel.getBuild()).thenReturn(mockBuild);
    assertFalse(CloudSdkUtils.hasGcloudMavenPlugin(mockModel));
  }

  /**
   * Tests that {@link CloudSdkUtils#hasGcloudMavenPlugin(Model)} returns
   * true when {@code Model} has the gcloud-maven-plugin.
   */
  @Test
  public void testHasGcloudMavenPlugin_withGcloudPlugin() {
    List<Plugin> plugins = createPluginList("myArtifactId", "myGroupId", "gcloud-maven-plugin",
        "com.google.appengine");
    when(mockBuild.getPlugins()).thenReturn(plugins);
    when(mockModel.getBuild()).thenReturn(mockBuild);

    assertTrue(CloudSdkUtils.hasGcloudMavenPlugin(mockModel));
  }

  @Test
  public void testHasGcloudMavenPlugin_wrongArtifactId() {
    List<Plugin> plugins = createPluginList("myArtifactId", "myGroupId", "this-is-not-gcloud-maven-plugin",
        "com.google.appengine");
    when(mockBuild.getPlugins()).thenReturn(plugins);
    when(mockModel.getBuild()).thenReturn(mockBuild);

    assertFalse(CloudSdkUtils.hasGcloudMavenPlugin(mockModel));
  }

  // FacetedProjectNature.NATURE_ID is not API, but used by the actual implementation 
  // that is mocked here
  @SuppressWarnings("restriction")
  @Test
  public void testHasCloudSdkFacet_nonFacetProject() throws CoreException {
    when(mockProject.isAccessible()).thenReturn(true);
    when(mockProject.isNatureEnabled(FacetedProjectNature.NATURE_ID)).thenReturn(false);
    assertFalse(CloudSdkUtils.hasCloudSdkFacet(mockProject));
  }
  
  private List<Plugin> createPluginList(String artifactId1, String groupId1, String artifactId2,
      String groupId2) {
    Plugin plugin1 = new Plugin();
    plugin1.setArtifactId(artifactId1);
    plugin1.setGroupId(groupId1);

    Plugin plugin2 = new Plugin();
    plugin2.setArtifactId(artifactId2);
    plugin2.setGroupId(groupId2);

    List<Plugin> plugins = new ArrayList<Plugin>();
    plugins.add(plugin1);
    plugins.add(plugin2);

    return plugins;
  }
}
