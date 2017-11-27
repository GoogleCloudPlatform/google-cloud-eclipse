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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.Base64;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.Iam.Projects;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts.Keys;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts.Keys.Create;
import com.google.api.services.iam.v1.model.CreateServiceAccountKeyRequest;
import com.google.api.services.iam.v1.model.ServiceAccountKey;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

  @Mock private Credential credential;
  @Mock private IGoogleApiFactory apiFactory;

  private Path keyFile;

  @Before
  public void setUp() {
    keyFile = tempFolder.getRoot().toPath().resolve("key.json");
  }

  public static void setUpServiceKeyCreation(
      IGoogleApiFactory mockApiFactory, boolean throwException) throws IOException {
    Iam iam = mock(Iam.class);
    Projects projects = mock(Projects.class);
    ServiceAccounts serviceAccounts = mock(ServiceAccounts.class);
    Keys keys = mock(Keys.class);
    Create create = mock(Create.class);

    ServiceAccountKey serviceAccountKey = new ServiceAccountKey();
    byte[] keyContent = "key data in JSON format".getBytes();
    serviceAccountKey.setPrivateKeyData(Base64.encodeBase64String(keyContent));

    when(mockApiFactory.newIamApi(any(Credential.class))).thenReturn(iam);
    when(iam.projects()).thenReturn(projects);
    when(projects.serviceAccounts()).thenReturn(serviceAccounts);
    when(serviceAccounts.keys()).thenReturn(keys);
    when(keys.create(anyString(), any(CreateServiceAccountKeyRequest.class))).thenReturn(create);

    if (throwException) {
      when(create.execute()).thenThrow(new IOException("log from unit test"));
    } else {
      when(create.execute()).thenReturn(serviceAccountKey);
    }
  }

  @Test
  public void testCreateServiceAccountKey_destinationShouldBeAbsolute() throws IOException {
    try {
      ServiceAccountUtil.createServiceAccountKey(apiFactory, credential, "my-project",
          "my-service-account@example.com", Paths.get("relative/path/to.json"));
    } catch (IllegalArgumentException e) {
      assertEquals("destination not absolute", e.getMessage());
    }
  }

  @Test
  public void testCreateServiceAccountKey() throws IOException {
    setUpServiceKeyCreation(apiFactory, false);

    ServiceAccountUtil.createServiceAccountKey(apiFactory, credential, "my-project",
        "my-service-account@example.com", keyFile);

    byte[] bytesRead = Files.readAllBytes(keyFile);
    assertEquals("key data in JSON format", new String(bytesRead, StandardCharsets.UTF_8));
  }

  @Test
  public void testCreateServiceAccountKey_replacesExistingFile() throws IOException {
    setUpServiceKeyCreation(apiFactory, false);

    Files.write(keyFile, new byte[] {0, 1, 2});
    ServiceAccountUtil.createServiceAccountKey(apiFactory, credential, "my-project",
        "my-service-account@example.com", keyFile);

    byte[] bytesRead = Files.readAllBytes(keyFile);
    assertEquals("key data in JSON format", new String(bytesRead, StandardCharsets.UTF_8));
  }

  @Test
  public void testCreateServiceAccountKey_ioException() throws IOException {
    setUpServiceKeyCreation(apiFactory, true);

    try {
      ServiceAccountUtil.createServiceAccountKey(apiFactory, credential, "my-project",
          "my-service-account@example.com", keyFile);
    } catch (IOException e) {
      assertEquals("log from unit test", e.getMessage());
    }
    assertFalse(Files.exists(keyFile));
  }

  @Test
  public void testCreateServiceAccountKey_createsRequiredDirectories() throws IOException {
    setUpServiceKeyCreation(apiFactory, false);

    Path keyFile = tempFolder.getRoot().toPath().resolve("non/existing/directory/key.json");
    ServiceAccountUtil.createServiceAccountKey(apiFactory, credential, "my-project",
          "my-service-account@example.com", keyFile);

    byte[] bytesRead = Files.readAllBytes(keyFile);
    assertEquals("key data in JSON format", new String(bytesRead, StandardCharsets.UTF_8));
  }
}
