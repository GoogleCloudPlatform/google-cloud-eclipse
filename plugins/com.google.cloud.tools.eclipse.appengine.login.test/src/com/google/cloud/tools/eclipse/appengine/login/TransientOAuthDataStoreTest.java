package com.google.cloud.tools.eclipse.appengine.login;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.ide.login.OAuthData;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class TransientOAuthDataStoreTest {

  @Mock IEclipseContext eclipseContext;

  @Test
  public void testLoadOAuthData_emptyStoreReturnsNonNullOAuthData() {
    when(eclipseContext.get(anyString())).thenReturn(null);

    OAuthData oAuthData = new TransientOAuthDataStore(eclipseContext).loadOAuthData();
    Assert.assertNotNull(oAuthData);
    Assert.assertEquals(null, oAuthData.getAccessToken());
    Assert.assertEquals(null, oAuthData.getRefreshToken());
    Assert.assertEquals(null, oAuthData.getStoredEmail());
    Assert.assertEquals(0, oAuthData.getAccessTokenExpiryTime());
  }

  OAuthData singleStorageForIEclipseContext;

  @Test
  public void testSaveAndLoadOAuthData() {
    // Set up IEclipseContext so that it returns OAuthData that was passed and saved.
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) {
        singleStorageForIEclipseContext = invocation.getArgumentAt(1, OAuthData.class);
        return null;
      }
    }).doThrow(Exception.class).when(eclipseContext).set(anyString(), any());
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        return singleStorageForIEclipseContext;
      }
    }).when(eclipseContext).get(anyString());

    OAuthData inputData = mock(OAuthData.class);
    TransientOAuthDataStore dataStore = new TransientOAuthDataStore(eclipseContext);
    dataStore.saveOAuthData(inputData);

    Assert.assertEquals(inputData, dataStore.loadOAuthData());
  }
}
