package com.google.cloud.tools.eclipse.appengine.localserver.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudSdkServerTest {

  @Mock private IServer server;
  @Mock private CloudSdkServer mockCloudSdkServer;

  @Test
  public void testGetCloudSdkServer_hasAdapter() {
    when(server.getAdapter(CloudSdkServer.class)).thenReturn(mockCloudSdkServer);
    CloudSdkServer cloudSdkServer = CloudSdkServer.getCloudSdkServer(server);
    assertThat(cloudSdkServer, sameInstance(mockCloudSdkServer));
  }

  @Test
  public void testGetCloudSdkServer_needsToLoadAdapter() {
    when(server.getAdapter(CloudSdkServer.class)).thenReturn(null);
    when(server.loadAdapter(eq(CloudSdkServer.class),
                            any(IProgressMonitor.class))).thenReturn(mockCloudSdkServer);
    CloudSdkServer cloudSdkServer = CloudSdkServer.getCloudSdkServer(server);
    assertThat(cloudSdkServer, sameInstance(mockCloudSdkServer));
  }
}
