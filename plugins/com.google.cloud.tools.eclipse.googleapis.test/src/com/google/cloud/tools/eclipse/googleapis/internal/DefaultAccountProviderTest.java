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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.googleapis.Account;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;


@SuppressWarnings("DoNotMock")
@RunWith(MockitoJUnitRunner.class)
public class DefaultAccountProviderTest {
  
  TestDefaultAccountProvider provider;
  final String EMAIL_1 = "email1";
  final String TOKEN_1 = "token1";
  final String NAME_1 = "name1";
  final String AVATAR_1 = "avatar1";
  
  final String EMAIL_2 = "email2";
  final String TOKEN_2 = "token2";
  final String NAME_2 = "name2";
  final String AVATAR_2 = "avatar2";
  private CredentialChangeListener listener;
  
  static final String TEMP_ADC_FILENAME = "test_adc.json";
  private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
  
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  
  @Before
  public void setup() throws IOException {
    LOGGER.info("setup()");
    LOGGER.info("Temp folder location: " + tempFolder.getRoot().toPath().toString());
    provider = new TestDefaultAccountProvider(tempFolder.newFile(TEMP_ADC_FILENAME).toPath());
    Account acct1 = new Account(EMAIL_1, mock(Credential.class), NAME_1, AVATAR_1);
    Account acct2 = new Account(EMAIL_2, mock(Credential.class), NAME_2, AVATAR_2);
    provider.addAccount(TOKEN_1, acct1);
    provider.addAccount(TOKEN_2, acct2);
    logout();
    listener = new CredentialChangeListener();
    provider.addCredentialChangeListener(listener::onFileChanged);
  }
  
  @Test
  public void testFileWriteTriggersListeners() throws InterruptedException {
    login(TOKEN_1);
    listener.waitUntilChange(1);
//    assertEquals(2, provider.getNumberOfCredentialChangeChecks());
//    assertEquals(2, provider.getNumberOfCredentialPropagations());
    login(TOKEN_2);
    listener.waitUntilChange(2);
    assertEquals(3, provider.getNumberOfCredentialChangeChecks());
    assertEquals(3, provider.getNumberOfCredentialPropagations());
    provider.removeCredentialChangeListener(listener::onFileChanged);
    login(TOKEN_1);
    Thread.sleep(1000);
    assertEquals(4, provider.getNumberOfCredentialChangeChecks());
    assertEquals(4, provider.getNumberOfCredentialPropagations());
    assertEquals(2, listener.getCallCount());
    logout();
    Thread.sleep(1000);
    assertEquals(5, provider.getNumberOfCredentialChangeChecks());
    assertEquals(5, provider.getNumberOfCredentialPropagations());
    assertEquals(2, listener.getCallCount());
  }
  
  @Test
  public void testFileWriteChangesReturnedAccount() {
    Optional<Account> acct = provider.getAccount();
    assertFalse(acct.isPresent());
    
    login(TOKEN_1);
    listener.waitUntilChange(1);
    acct = provider.getAccount();
    assertTrue(acct.isPresent());
    assertEquals(EMAIL_1, acct.get().getEmail());
    assertEquals(TOKEN_1, ((CredentialWithId) acct.get().getOAuth2Credential()).getId());
    
    login(TOKEN_2);
    listener.waitUntilChange(2);
    acct = provider.getAccount();
    assertTrue(acct.isPresent());
    assertEquals(EMAIL_2, acct.get().getEmail());
    assertEquals(TOKEN_2, ((CredentialWithId) acct.get().getOAuth2Credential()).getId());
    
    logout();
    listener.waitUntilChange(3);
    assertFalse(acct.isPresent());
  }
  
  @Test
  public void testFileWriteChangesReturnedCredential() {
    Optional<Credential> cred = provider.getCredential();
    assertFalse(cred.isPresent());
    
    login(TOKEN_1);
    listener.waitUntilChange(1);
    cred = provider.getCredential();
    assertTrue(cred.isPresent());
    assertEquals(TOKEN_1, ((CredentialWithId) cred.get()).getId());
    
    login(TOKEN_2);
    listener.waitUntilChange(2);
    cred = provider.getCredential();
    assertTrue(cred.isPresent());
    assertEquals(TOKEN_2, ((CredentialWithId) cred.get()).getId());
    
    logout();
    listener.waitUntilChange(3);
    assertFalse(cred.isPresent());
  }
  
  
  private Path getTempAdcPath() {
    Path result = tempFolder.getRoot().toPath().resolve(TEMP_ADC_FILENAME);
    LOGGER.info("getTempAdcPath(): " + result.toString());
    return result;
  }
  
  private void login(String token) {
    LOGGER.info("login(" + token + ")");
    try {
      File adcFile = getTempAdcPath().toFile();
      LOGGER.info("login() adcFile path: " + adcFile.toPath().toString());
      if (!adcFile.exists()) {
          LOGGER.info("login() creating new file");
          adcFile = tempFolder.newFile(TEMP_ADC_FILENAME);
      }
      LOGGER.info(
          "login() pre-write contents: " + Files.readAllLines(adcFile.toPath())
          .stream().collect(Collectors.joining()));
      assertTrue(adcFile.exists());
      try (FileWriter writer = new FileWriter(adcFile)) {
        writer.write(token);
      }
      LOGGER.info(
          "login() post-write contents: " + Files.readAllLines(adcFile.toPath())
          .stream().collect(Collectors.joining()));
    } catch (IOException ex) {
      fail(ex.getMessage());
    }
  }
  
  private void logout() {
    LOGGER.info("logout()");
    File adcFile = getTempAdcPath().toFile();
    if (adcFile.exists()) {
      LOGGER.info("logout() delete file");
      adcFile.delete();
    }
  }
  
  public class CredentialChangeListener {

    private int callCount = 0;
    private static final int WAIT_INTERVAL_MS = 100;
    private static final long DEFAULT_WAIT_INTERVAL_MS = 5000;
    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    public synchronized void onFileChanged() {
      callCount++;
      LOGGER.info(this.hashCode() + ": callCount increased to " + callCount);
    }

    public synchronized int getCallCount() {
        return callCount;
    }
    
    public void waitUntilChange(int expectedCallCount) {
      waitUntilChange(DEFAULT_WAIT_INTERVAL_MS, expectedCallCount);
    }
    
    private void waitUntilChange(long timeoutMs, int expectedCallCount) {
      final int initialCallCount = getCallCount();
      LOGGER.info(this.hashCode() + ": initialCallCount: " + initialCallCount + ", expectedCallCount: " + expectedCallCount);
      if (initialCallCount == expectedCallCount) {
        LOGGER.info(this.hashCode() + ": Already on expected call count");
        return;
      }
      else if (initialCallCount > expectedCallCount) {
        fail("already called more times than expected");
      }
      long msWaited = 0;
      while (msWaited < timeoutMs) {
        try {
          msWaited += WAIT_INTERVAL_MS;
          Thread.sleep(WAIT_INTERVAL_MS);
          LOGGER.info(this.hashCode() + " (loop): initialCallCount: " + initialCallCount + ", callCount: " + expectedCallCount);
          if (initialCallCount != getCallCount()) {
            assertEquals(expectedCallCount, getCallCount());
          }
        } catch (InterruptedException ex) {
          LOGGER.info("interrupted");
          continue;
        }
      }
      fail("Timeout of " + timeoutMs + " exceeded");
    }

}
  
  public class TestDefaultAccountProvider extends DefaultAccountProvider {
    
    final Map<String, Account> accountMap = new HashMap<>();
    private int numberOfCredentialChangeChecks = 0;
    private int numberOfCredentialPropagations = 0;
    private String currentToken = "";
    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
    
        
    public TestDefaultAccountProvider(Path adcPath) {
      super(adcPath);
    }
    
    public int getNumberOfCredentialChangeChecks() {
      return numberOfCredentialChangeChecks;
    }
    
    public int getNumberOfCredentialPropagations() {
      return numberOfCredentialPropagations;
    }
    
    @Override
    protected void confirmAdcCredsChanged() {
      LOGGER.info("checking if creds changed");
      numberOfCredentialChangeChecks++;
      String newToken = getRefreshTokenFromCredentialFile();
      LOGGER.info("currentToken: " + currentToken + ", newToken: " + newToken);
      if (newToken.compareTo(currentToken) != 0) {
        currentToken = newToken;
        propagateCredentialChange();
      }
    }
    
    @Override
    protected void propagateCredentialChange() {
      LOGGER.info("propagating credentials change");
      numberOfCredentialPropagations++;
      super.propagateCredentialChange();
    }
    
    public void addAccount(String id, Account acct) {
      accountMap.put(id, acct);
    }
    
    public TemporaryFolder getTempFolder() {
      return tempFolder;
    }
    
    /**
     * @return the application default credentials associated account
     */
    @Override
    public Optional<Account> getAccount(){
      return computeAccount();
    }
    
    @Override
    protected Optional<Account> computeAccount() {
      Optional<Credential> cred = getCredential();
      if (!cred.isPresent() || !accountMap.containsKey(((CredentialWithId)cred.get()).getId())) {
        return Optional.empty();
      }
      Account data = accountMap.get(((CredentialWithId)cred.get()).getId());
      return Optional.of(new Account(
          data.getEmail(),
          cred.get(),
          data.getName().orElse(null),
          data.getAvatarUrl().orElse(null)
      ));
    }
    
    private String getFileContents() {
      File credsFile = getCredentialFile();
      if (!credsFile.exists()) {
        LOGGER.info("credsFile does not exist at location: " + credsFile.getAbsolutePath());
        return "";
      }
      try {
        String content = Files.readAllLines(credsFile.toPath())
            .stream()
            .collect(Collectors.joining("\n"));
        LOGGER.info("content at " + credsFile.getAbsolutePath() + ": " + content);
        return content;
      } catch (IOException ex) {
        fail();
      }
      return "";
    }
    
    @Override
    protected String getRefreshTokenFromCredentialFile() {
      String result = getFileContents();
      LOGGER.info("getRefreshTokenFromCredentialFile(): " + result);
      return result;
    }
    
    @Override
    protected Optional<Credential> computeCredential() {
      File credsFile = getCredentialFile();
      if (!credsFile.exists()) { 
        return Optional.empty();
      }
      CredentialWithId cred = mock(CredentialWithId.class);
      when(cred.getId()).thenReturn(getFileContents());
      return Optional.of(cred);
    }
  }
  
  public class CredentialWithId extends Credential {
    
    protected CredentialWithId(Builder builder) {
      super(builder);
    }

    public String getId() {
      // should be overriden by mock
      return null;
    }
  }
}
