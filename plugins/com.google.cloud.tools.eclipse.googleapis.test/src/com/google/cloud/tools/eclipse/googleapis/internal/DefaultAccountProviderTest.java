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
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.cloud.tools.eclipse.googleapis.Account;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
  
  static final String TEMP_ADC_FILENAME = "test_adc.json";
  
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  
  @Before
  public void setup() {
    System.out.println("Temp folder location: " + tempFolder.getRoot().toPath().toString());
    provider = new TestDefaultAccountProvider();
    Account acct1 = new Account(EMAIL_1, mock(Credential.class), NAME_1, AVATAR_1);
    Account acct2 = new Account(EMAIL_2, mock(Credential.class), NAME_2, AVATAR_2);
    provider.addAccount(TOKEN_1, acct1);
    provider.addAccount(TOKEN_2, acct2);
    logout();
  }
  
  @Test
  public void testFileWriteTriggersListeners() {
    CredentialChangeListener listener = new CredentialChangeListener();
    provider.addCredentialChangeListener(listener::onFileChanged);
    login(TOKEN_1);
    assertEquals(2, provider.getNumberOfCredentialChangeChecks());
    assertEquals(2, provider.getNumberOfCredentialPropagations());
    assertEquals(1, listener.getCallCount());
    login(TOKEN_2);
    assertEquals(3, provider.getNumberOfCredentialChangeChecks());
    assertEquals(3, provider.getNumberOfCredentialPropagations());
    assertEquals(2, listener.getCallCount());
    provider.removeCredentialChangeListener(listener::onFileChanged);
    login(TOKEN_1);
    assertEquals(4, provider.getNumberOfCredentialChangeChecks());
    assertEquals(4, provider.getNumberOfCredentialPropagations());
    assertEquals(2, listener.getCallCount());
    logout();
    assertEquals(5, provider.getNumberOfCredentialChangeChecks());
    assertEquals(5, provider.getNumberOfCredentialPropagations());
    assertEquals(2, listener.getCallCount());
  }
  
  @Test
  public void testFileWriteChangesReturnedAccount() {
    Optional<Account> acct = provider.getAccount();
    assertFalse(acct.isPresent());
    
    login(TOKEN_1);
    acct = provider.getAccount();
    assertTrue(acct.isPresent());
    assertEquals(EMAIL_1, acct.get().getEmail());
    assertEquals(TOKEN_1, acct.get().getOAuth2Credential().getRefreshToken());
    
    login(TOKEN_2);
    acct = provider.getAccount();
    assertTrue(acct.isPresent());
    assertEquals(EMAIL_2, acct.get().getEmail());
    assertEquals(TOKEN_2, acct.get().getOAuth2Credential().getRefreshToken());
    
    logout();
    assertFalse(acct.isPresent());
  }
  
  @Test
  public void testFileWriteChangesReturnedCredential() {
    Optional<Credential> cred = provider.getCredential();
    assertFalse(cred.isPresent());
    
    login(TOKEN_1);
    cred = provider.getCredential();
    assertTrue(cred.isPresent());
    assertEquals(TOKEN_1, cred.get().getRefreshToken());
    
    login(TOKEN_2);
    cred = provider.getCredential();
    assertTrue(cred.isPresent());
    assertEquals(TOKEN_2, cred.get().getRefreshToken());
    
    logout();
    assertFalse(cred.isPresent());
  }
  
  
  private Path getTempAdcPath() {
    Path result = tempFolder.getRoot().toPath().resolve(TEMP_ADC_FILENAME);
    System.out.println("getTempAdcPath(): " + result.toString());
    return result;
  }
  
  private void login(String token) {
    try {
      File adcFile = getTempAdcPath().toFile();
      System.out.println("login() adcFile path: " + adcFile.toPath().toString());
      if (!adcFile.exists()) {
          System.out.println("login() creating new file");
          adcFile = tempFolder.newFile(TEMP_ADC_FILENAME);
      }
      System.out.println(
          "login() pre-write contents: " + Files.readAllLines(adcFile.toPath())
          .stream().collect(Collectors.joining()));
      assertTrue(adcFile.exists());
      try (FileWriter writer = new FileWriter(adcFile)) {
        writer.write(token);
      }
      System.out.println(
          "login() post-write contents: " + Files.readAllLines(adcFile.toPath())
          .stream().collect(Collectors.joining()));
    } catch (IOException ex) {
      fail(ex.getMessage());
    }
  }
  
  private void logout() {
    File adcFile = getTempAdcPath().toFile();
    if (adcFile.exists()) {
      adcFile.delete();
    }
  }
  
  public class CredentialChangeListener {

    private int callCount = 0;

    public void onFileChanged() {
        callCount++;
    }

    public int getCallCount() {
        return callCount;
    }

}
  
  public class TestDefaultAccountProvider extends DefaultAccountProvider {
    
    final Map<String, Account> accountMap = new HashMap<>();
    private int numberOfCredentialChangeChecks = 0;
    private int numberOfCredentialPropagations = 0;
    
        
    public TestDefaultAccountProvider() {
      try {
        ADC_PATH = tempFolder.newFile(TEMP_ADC_FILENAME).toPath();
      } catch (IOException ex) {
        fail();
      }
      initWatchService();
    }
    
    public int getNumberOfCredentialChangeChecks() {
      return numberOfCredentialChangeChecks;
    }
    
    public int getNumberOfCredentialPropagations() {
      return numberOfCredentialPropagations;
    }
    
    @Override
    protected void confirmAdcCredsChanged() {
      numberOfCredentialChangeChecks++;
      super.confirmAdcCredsChanged();
    }
    
    @Override
    protected void propagateCredentialChange() {
      numberOfCredentialPropagations++;
      super.propagateCredentialChange();
    }
    
    public void addAccount(String refreshToken, Account acct) {
      accountMap.put(refreshToken, acct);
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
      if (!cred.isPresent() || !accountMap.containsKey(cred.get().getRefreshToken())) {
        return Optional.empty();
      }
      Account data = accountMap.get(cred.get().getRefreshToken());
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
        return "";
      }
      try {
        return Files.readAllLines(credsFile.toPath())
            .stream()
            .collect(Collectors.joining("\n"));
      } catch (IOException ex) {
        fail();
      }
      return "";
    }
    
    @Override
    protected String getRefreshTokenFromCredentialFile() {
      return getFileContents();
    }
    
    @Override
    protected Optional<Credential> computeCredential() {
      File credsFile = getCredentialFile();
      if (!credsFile.exists()) { 
        return Optional.empty();
      }
      GoogleCredential cred = mock(GoogleCredential.class);
      when(cred.getRefreshToken()).thenReturn(getFileContents());
      return Optional.of(cred);
    }
  }
}
