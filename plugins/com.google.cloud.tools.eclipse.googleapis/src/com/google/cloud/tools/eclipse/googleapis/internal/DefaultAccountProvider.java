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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.cloud.tools.eclipse.googleapis.Account;
import com.google.cloud.tools.eclipse.googleapis.IAccountProvider;
import com.google.cloud.tools.eclipse.googleapis.UserInfo;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
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
  
  private Map<Credential, Account> accountCache = new HashMap<>();
  
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
      return Optional.of(getAccount(GoogleCredential.getApplicationDefault()));
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "IOException occurred when obtaining ADC", ex);
      return Optional.empty();
    }
    
  }
  
  /**
   * @return the ADC if set
   */
  @Override
  public Optional<Credential> getCredential() {
    Optional<Account> account = getAccount();
    if (account.isPresent()) {
      return Optional.of(account.get().getOAuth2Credential());
    }
    return Optional.empty();
  }
  
  Account getAccount(Credential credential) throws IOException {
    if (accountCache.containsKey(credential)) {
      return accountCache.get(credential);
    }
    HttpRequestInitializer chainedInitializer = new HttpRequestInitializer() {
      @Override
      public void initialize(HttpRequest httpRequest) throws IOException {
        credential.initialize(httpRequest);
        requestTimeoutSetter.initialize(httpRequest);
      }
    };
    
    Oauth2 oauth2 = new Oauth2.Builder(transport, jsonFactory, credential)
        .setHttpRequestInitializer(chainedInitializer)
        .setApplicationName(CloudToolsInfo.USER_AGENT)
        .build();
    
    UserInfo userInfo = new UserInfo(oauth2.userinfo().get().execute());
    Account result = new Account(userInfo.getEmail(), credential, userInfo.getName(), userInfo.getPicture());
    accountCache.put(credential, result);
    return result;
  }
}