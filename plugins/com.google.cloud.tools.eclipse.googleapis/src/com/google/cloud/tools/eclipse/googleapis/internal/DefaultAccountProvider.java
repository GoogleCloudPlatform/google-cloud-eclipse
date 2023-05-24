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
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
public class DefaultAccountProvider extends AccountProvider {

  protected Path ADC_PATH = Paths.get(GoogleAuthUtils.getWellKnownCredentialsPath()).toAbsolutePath();
  
  public static final DefaultAccountProvider INSTANCE = new DefaultAccountProvider();
  private static final int USER_INFO_QUERY_HTTP_CONNECTION_TIMEOUT = 5000 /* ms */;
  private static final int USER_INFO_QUERY_HTTP_READ_TIMEOUT = 3000 /* ms */;
  private static final HttpTransport transport = new NetHttpTransport();
   
  private final JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
  private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
  
  private Optional<Credential> currentCred = computeCredential();
  private Optional<Account> cachedAccount = Optional.empty();
  private WatchService watchService;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  
  
  protected DefaultAccountProvider() {
    initWatchService();
  }
  
  protected void initWatchService() {
    Path adcFolderPath = ADC_PATH.getParent();
    try {
      watchService = FileSystems.getDefault().newWatchService();
      adcFolderPath.register(watchService, 
          StandardWatchEventKinds.ENTRY_MODIFY,
          StandardWatchEventKinds.ENTRY_DELETE,
          StandardWatchEventKinds.ENTRY_CREATE);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Error creating watch service", ex);
    }
    executorService.execute(() -> {
      while (true) {
        WatchKey key;
        try {
          key = watchService.take();
        } catch (InterruptedException ex) {
          LOGGER.log(Level.SEVERE, "Error creating watch service", ex);
          continue;
        }
        for (WatchEvent<?> event : key.pollEvents()) {
          LOGGER.log(Level.FINE, "Detected change in ADC file");
          Path affectedFile = Paths.get(adcFolderPath.toAbsolutePath().toString(), 
              ((Path) event.context()).toString()).toAbsolutePath();
          if (affectedFile.equals(ADC_PATH)) {
            confirmAdcCredsChanged();
            break; // prevent propagation for two events on same file and different kind
          }
        }
        key.reset();
      }
    });
  }
  
  protected void confirmAdcCredsChanged() {
    String newToken = getRefreshTokenFromCredentialFile();
    String currtoken = currentCred.map(Credential::getRefreshToken).orElse("");
    if (newToken.compareTo(currtoken) != 0) {
      currentCred = computeCredential();
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
  
  protected Optional<Account> computeAccount() {
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
  
  protected Optional<Credential> computeCredential() {
    File credsFile = getCredentialFile();
    if (!credsFile.exists()) { 
      return Optional.empty();
    }
    try (FileInputStream credsStream = new FileInputStream(credsFile)) {
      return Optional.ofNullable(GoogleCredential.fromStream(credsStream));
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Error when computing credentials from ADC file", ex);
      return Optional.empty();
    }
  }
  
  protected File getCredentialFile() {
    return ADC_PATH.toFile();
  }
  
  /**
   * Manually reads the refresh token from the credential file. 
   * This saves a server trip that obtains an access token when instantiating credentials
   * @return refresh token from ADC well-known file
   */
  protected String getRefreshTokenFromCredentialFile() {
    File credsFile = getCredentialFile();
    if (!credsFile.exists()) {
      return "";
    }
    
    try (JsonReader reader = new JsonReader(new FileReader(credsFile))) {
      JsonElement root = JsonParser.parseReader(reader);
      return root.getAsJsonObject().get("refresh_token").getAsString();
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Could not open credentials file");
    }
    return "";
     
    
  }
}
