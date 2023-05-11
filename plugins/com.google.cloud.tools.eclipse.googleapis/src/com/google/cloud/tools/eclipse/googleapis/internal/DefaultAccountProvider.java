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
import com.google.auth.oauth2.GoogleAuthUtils;
import com.google.cloud.tools.eclipse.googleapis.Account;
import com.google.cloud.tools.eclipse.googleapis.UserInfo;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
public class DefaultAccountProvider extends AccountProvider {

  private static final String ADC_PATH = GoogleAuthUtils.getWellKnownCredentialsPath();
  public static final DefaultAccountProvider INSTANCE = new DefaultAccountProvider();
  private static final int USER_INFO_QUERY_HTTP_CONNECTION_TIMEOUT = 5000 /* ms */;
  private static final int USER_INFO_QUERY_HTTP_READ_TIMEOUT = 3000 /* ms */;
  private static final HttpTransport transport = new NetHttpTransport();
   
  private final JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
  private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
  
  private Optional<Credential> currentCred = computeCredential();
  private Optional<Account> cachedAccount = Optional.empty();
  private Timer adcPathPoller;
  
  private DefaultAccountProvider() {
    adcPathPoller = new Timer();
    TimerTask task = new TimerTask() {
      @Override
      public void run() {
          checkIfAdcPathChanged();
      }
    };

    // Schedule the task to run every second
    adcPathPoller.scheduleAtFixedRate(task, 0, 1000);
  }
  
  private final void checkIfAdcPathChanged() {
    Optional<Credential> newCred = computeCredential();
    String newToken = newCred.map(Credential::getRefreshToken).orElse("");
    String currtoken = currentCred.map(Credential::getRefreshToken).orElse("");
    if (newToken.compareTo(currtoken) != 0) {
      currentCred = newCred;
      cachedAccount = Optional.empty(); // lazily recompute the account
      propagateCredentialChange();
    }
  }
  
  
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
    return INSTANCE.computeAccount();
  }
  
  private Optional<Account> computeAccount() {
    if (!currentCred.isPresent()) {
      return Optional.empty();
    }
    if (cachedAccount.isPresent()) {
      return cachedAccount;
    }
    Credential credential = currentCred.get();
    try {
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
      Optional<Account> result = Optional.of(new Account(
          userInfo.getEmail(), credential, userInfo.getName(), userInfo.getPicture()));
      cachedAccount = result;
      return result;
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Error when computing account from ADC file", ex);
      return Optional.empty();
    }
  }
  
  /**
   * @return the ADC if set
   */
  @Override
  public Optional<Credential> getCredential() {
    return currentCred;
  }
  
  private Optional<Credential> computeCredential() {
    File credsFile = getCredentialFile();
    try (FileInputStream credsStream = new FileInputStream(credsFile)) {
      return Optional.ofNullable(GoogleCredential.fromStream(credsStream));
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Error when computing credentials from ADC file", ex);
      return Optional.empty();
    }
  }
  
  private Optional<File> getCredentialFile() {
    File credsFile = new File(ADC_PATH);
    if (!credsFile.exists()) {
      return Optional.empty();
    }
    return Optional.of(credsFile);
  }
}
