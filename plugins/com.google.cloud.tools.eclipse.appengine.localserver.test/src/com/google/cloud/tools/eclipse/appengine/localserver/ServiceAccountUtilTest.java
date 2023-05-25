/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.localserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.util.Base64;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.Iam.Projects;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts.Keys;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts.Keys.Create;
import com.google.api.services.iam.v1.model.CreateServiceAccountKeyRequest;
import com.google.api.services.iam.v1.model.ServiceAccountKey;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAccountUtilTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock private GoogleApiFactory apiFactory;
  @Mock private Keys keys;
  @Mock private Create create;

  private Path keyFile;

  @Before
  public void setUp() throws IOException {
    keyFile = tempFolder.getRoot().toPath().resolve("key.json");

    Iam iam = mock(Iam.class);
    Projects projects = mock(Projects.class);
    ServiceAccounts serviceAccounts = mock(ServiceAccounts.class);
    
    when(apiFactory.getAccount()).thenReturn(Optional.of(TestAccountProvider.ACCOUNT_1));
    when(apiFactory.getCredential()).thenReturn(Optional.of(TestAccountProvider.CREDENTIAL_ACCOUNT_1));
    when(apiFactory.newIamApi()).thenReturn(iam);
    GoogleApiFactory.setInstance(apiFactory);
    when(iam.projects()).thenReturn(projects);
    when(projects.serviceAccounts()).thenReturn(serviceAccounts);
    when(serviceAccounts.keys()).thenReturn(keys);
    when(keys.create(
        eq("projects/my-project/serviceAccounts/my-project@appspot.gserviceaccount.com"),
        any(CreateServiceAccountKeyRequest.class))).thenReturn(create);

    ServiceAccountKey serviceAccountKey = new ServiceAccountKey();
    byte[] keyContent = "key data in JSON format".getBytes(StandardCharsets.UTF_8);
    serviceAccountKey.setPrivateKeyData(Base64.encodeBase64String(keyContent));
    
    when(create.execute()).thenReturn(serviceAccountKey);
  }

  @Test
  public void testCreateServiceAccountKey_destinationShouldBeAbsolute() throws IOException {
    try {
      ServiceAccountUtil.createAppEngineDefaultServiceAccountKey( 
          "my-project", Paths.get("relative/path/to.json"));
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("destination not absolute", e.getMessage());
    }
  }

  @Test
  public void testCreateServiceAccountKey() throws IOException {
    ServiceAccountUtil.createAppEngineDefaultServiceAccountKey( 
        "my-project", keyFile);

    byte[] bytesRead = Files.readAllBytes(keyFile);
    assertEquals("key data in JSON format", new String(bytesRead, StandardCharsets.UTF_8));
  }
  
  @Test
  public void testCreateServiceAccountKey_prefixedProject() throws IOException {
    when(keys.create(
        eq("projects/google.com:my-project/serviceAccounts/my-project.google.com@appspot.gserviceaccount.com"),
        any(CreateServiceAccountKeyRequest.class))).thenReturn(create);

    ServiceAccountUtil.createAppEngineDefaultServiceAccountKey( 
        "google.com:my-project", keyFile);

    byte[] bytesRead = Files.readAllBytes(keyFile);
    assertEquals("key data in JSON format", new String(bytesRead, StandardCharsets.UTF_8));
  }

  @Test
  public void testCreateServiceAccountKey_replacesExistingFile() throws IOException {

    Files.write(keyFile, new byte[] {0, 1, 2});
    ServiceAccountUtil.createAppEngineDefaultServiceAccountKey( 
        "my-project", keyFile);

    byte[] bytesRead = Files.readAllBytes(keyFile);
    assertEquals("key data in JSON format", new String(bytesRead, StandardCharsets.UTF_8));
  }

  @Test
  public void testCreateServiceAccountKey_ioException() throws IOException {

    when(create.execute()).thenThrow(new IOException("log from unit test"));
    
    try {
      ServiceAccountUtil.createAppEngineDefaultServiceAccountKey( 
          "my-project", keyFile);
      fail();
    } catch (IOException e) {
      assertEquals("log from unit test", e.getMessage());
    }
    assertFalse(Files.exists(keyFile));
  }

  @Test
  public void testCreateServiceAccountKey_createsRequiredDirectories() throws IOException {
    keyFile = tempFolder.getRoot().toPath().resolve("non/existing/directory/key.json");
    ServiceAccountUtil.createAppEngineDefaultServiceAccountKey( 
        "my-project", keyFile);

    byte[] bytesRead = Files.readAllBytes(keyFile);
    assertEquals("key data in JSON format", new String(bytesRead, StandardCharsets.UTF_8));
  }
}
