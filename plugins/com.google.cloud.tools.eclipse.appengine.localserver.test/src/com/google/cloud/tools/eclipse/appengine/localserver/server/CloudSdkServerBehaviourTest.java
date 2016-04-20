package com.google.cloud.tools.eclipse.appengine.localserver.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.ServerSocket;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudSdkServerBehaviourTest {

  @Mock private Server mockServer;
  @Mock private CloudSdkServer mockCloudSdkServer;
  @Captor private ArgumentCaptor<Integer> serverStateArgumentCaptor;
  
  @Test
  public void testCanStart_portInUse() throws Exception {
    CloudSdkServerBehaviour cloudSdkServerBehaviour = new CloudSdkServerBehaviour();
    setField(cloudSdkServerBehaviour, "server", mockServer);
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      when(mockServer.getAdapter(eq(CloudSdkServer.class))).thenReturn(mockCloudSdkServer);
      when(mockCloudSdkServer.getApiPort()).thenReturn(serverSocket.getLocalPort());
      setField(mockCloudSdkServer, "server", mockServer);
      IStatus status = cloudSdkServerBehaviour.canStart("run");
      assertThat(status.getSeverity(), is(IStatus.ERROR));
    }
  }
  
  @Test
  public void testSetServerStarted() throws Exception {
    CloudSdkServerBehaviour cloudSdkServerBehaviour = new CloudSdkServerBehaviour();
    setField(cloudSdkServerBehaviour, "server", mockServer);
    cloudSdkServerBehaviour.setServerStarted();
    verify(mockServer).setServerState(eq(IServer.STATE_STARTED));
  }
  
  private <T, V> void setField(T obj, String fieldName, V value) throws Exception {
    Field field = getField(obj.getClass(), fieldName);
    if (field == null) {
      throw new NoSuchFieldException(fieldName);
    } else {
      field.setAccessible(true);
      field.set(obj, value);
    }
  }
  
  private Field getField(Class<?> clazz, String fieldName) {
    try {
      return clazz.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      clazz = clazz.getSuperclass();
      if (clazz == null) {
        return null;
      } else {
        return getField(clazz, fieldName);
      }
    }
  }
}
