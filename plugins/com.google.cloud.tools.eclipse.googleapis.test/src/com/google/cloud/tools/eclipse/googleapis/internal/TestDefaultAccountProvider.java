package com.google.cloud.tools.eclipse.googleapis.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.cloud.tools.eclipse.googleapis.Account;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.rules.TemporaryFolder;

public class TestDefaultAccountProvider extends DefaultAccountProvider {

    private static final Logger LOGGER = Logger.getLogger(TestDefaultAccountProvider.class.getName());
    
    final Map<String, Account> accountMap = new HashMap<>();
    private int numberOfCredentialChangeChecks = 0;
    private int numberOfCredentialPropagations = 0;
    private String currentId = "";
    private TemporaryFolder tempFolder;
    
        
    public TestDefaultAccountProvider(Path adcPath, TemporaryFolder tempFolder) {
      super(adcPath);
      this.tempFolder = tempFolder;
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
      String newId = getRefreshTokenFromCredentialFile();
      LOGGER.info("currentToken: " + currentId + ", newToken: " + newId);
      if (newId.compareTo(currentId) != 0) {
        currentId = newId;
        currentCred = computeCredential();
        propagateCredentialChange();
      }
    }
    
    /**
     * Waiting logic in {@link CredentialChangeListener} also uses main thread
     * This override uses the listener thread to simultaneously wait and updated the creds object
     */
    @Override
    protected void onAdcFileChanged() {
      confirmAdcCredsChanged();
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
      if (!cred.isPresent()) {
        LOGGER.info("computeAccount(): credential is not present");
        return Optional.empty();
      }
      String id = ((CredentialWithId)cred.get()).getId();
      if (!accountMap.containsKey(id)) {
        LOGGER.info("computeAccount(): accountMap does not contain ID: " + id);
        return Optional.empty();
      }
      Account data = accountMap.get(id);
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
        assertNotNull(LOGGER);
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
      if (Strings.isNullOrEmpty(currentId)) { 
        LOGGER.info("computeCredential: currentToken is empty");
        return Optional.empty();
      }
      LOGGER.info("computeCredential(): creating credential with ID: " + currentId);
      CredentialWithId cred = mock(CredentialWithId.class);
      when(cred.getId()).thenReturn(currentId);
      
      return Optional.of(cred);
    }
  }