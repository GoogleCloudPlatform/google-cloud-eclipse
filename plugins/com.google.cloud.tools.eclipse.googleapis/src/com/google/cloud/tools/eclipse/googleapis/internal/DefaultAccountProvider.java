/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.googleapis.internal;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential.AccessMethod;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.tools.eclipse.googleapis.Account;
import com.google.cloud.tools.eclipse.googleapis.IAccountProvider;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
public class DefaultAccountProvider implements IAccountProvider {

  private static final int USER_INFO_QUERY_HTTP_CONNECTION_TIMEOUT = 5000 /* ms */;
  private static final int USER_INFO_QUERY_HTTP_READ_TIMEOUT = 3000 /* ms */;
  private static final HttpTransport transport = new NetHttpTransport();
  
  private final JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
  private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
  
  private Map<Credentials, Account> accountCache = new HashMap<>();
  
  private static final HttpRequestInitializer requestTimeoutSetter = new HttpRequestInitializer() {
    @Override
    public void initialize(HttpRequest httpRequest) throws IOException {
      httpRequest.setConnectTimeout(USER_INFO_QUERY_HTTP_CONNECTION_TIMEOUT);
      httpRequest.setReadTimeout(USER_INFO_QUERY_HTTP_READ_TIMEOUT);
    }
  };
  
  /**
   * @return the application default credentials associated account
   */
  @Override
  public Optional<Account> getAccount(){
    try {
      return Optional.of(getAccount(GoogleCredentials.getApplicationDefault()));
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "IOException occurred when obtaining ADC", ex);
      return Optional.empty();
    }
    
  }
  
  /**
   * @return the ADC if set
   */
  @Override
  public Optional<Credentials> getCredential() {
    return getAccount().map(Account::getOAuth2Credential);
  }
  
  Account getAccount(final GoogleCredentials credential) throws IOException {
    if (accountCache.containsKey(credential)) {
      return accountCache.get(credential);
    }
//    HttpRequestInitializer chainedInitializer = new HttpRequestInitializer() {
//      @Override
//      public void initialize(HttpRequest httpRequest) throws IOException {
//        credential.initialize(httpRequest);
//        requestTimeoutSetter.initialize(httpRequest);
//      }
//    };
//    Oauth2 oauth2 = new Oauth2.Builder(transport, jsonFactory, credential)
//        .setHttpRequestInitializer(chainedInitializer)
//        .setApplicationName(CloudToolsInfo.USER_AGENT)
//        .build();
//    // oauth2.userinfo().get().execute() gets the user info
//    UserInfo userInfo = new UserInfo(oauth2.userinfo().get().execute());
    
//    if (credential.createScopedRequired()) {
//      credential = credential.createScoped(Arrays.asList("https://www.googleapis.com/auth/userinfo.email"));
//    }
    
    
    HttpRequestFactory requestFactory = transport.createRequestFactory(new HttpRequestInitializer() {
      @Override
      public void initialize(HttpRequest request) throws IOException {
        request.setInterceptor(new CredentialInterceptor(credential));
        request.setParser(jsonFactory.createJsonObjectParser());
      }
    });
    HttpResponse response = requestFactory.buildGetRequest(
        new GenericUrl("https://www.googleapis.com/oauth2/v3/userinfo"))
//        .setHeaders(new HttpHeaders().setAuthorization("Bearer " + accessToken))
        .execute();
    Userinfoplus userInfo = response.parseAs(Userinfoplus.class);
    Account result = new Account(userInfo.getEmail(), credential, userInfo.getName(), userInfo.getPicture());
    accountCache.put(credential, result);
    return result;
  }
  
  private class CredentialInterceptor implements HttpExecuteInterceptor {
    
    private GoogleCredentials credential;
    private AccessMethod method;
    
    public CredentialInterceptor(GoogleCredentials credential) {
      this.credential = credential;
      this.method = BearerToken.authorizationHeaderAccessMethod();
    }
    
    @Override
    public void intercept(HttpRequest request) throws IOException {
      // TODO Auto-generated method stub
      method.intercept(request, credential.getAccessToken().getTokenValue());
    }
    
  }
}
