/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.preferences;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.Buckets;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.project.VerifyStagingLocationJob.VerifyStagingLocationResult;
import com.google.cloud.tools.eclipse.dataflow.ui.page.MessageTarget;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.cloud.tools.login.Account;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RunOptionsDefaultsComponentTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  @Mock private DataflowPreferences preferences;
  @Mock private MessageTarget messageTarget;
  @Mock private IGoogleLoginService loginService;
  @Mock private IGoogleApiFactory apiFactory;

  private RunOptionsDefaultsComponent component;
  private Shell shell;

  private AccountSelector selector;
  private Text projectID;
  private Combo stagingLocations;
  private Button createButton;


  @Before
  public void setUp() throws IOException {
    Account account1 = mock(Account.class);
    Account account2 = mock(Account.class);
    Credential credential1 = mock(Credential.class);
    Credential credential2 = mock(Credential.class);

    when(account1.getEmail()).thenReturn("alice@example.com");
    when(account2.getEmail()).thenReturn("bob@example.com");
    when(account1.getOAuth2Credential()).thenReturn(credential1);
    when(account2.getOAuth2Credential()).thenReturn(credential2);

    when(loginService.getAccounts()).thenReturn(Sets.newHashSet(account1, account2));

    mockStorageApiBucketList(credential1, "alice-bucket-1", "alice-bucket-2");
    mockStorageApiBucketList(credential2, "bob-bucket");

    shell = shellResource.getShell();
    component = new RunOptionsDefaultsComponent(
        shell, 3, messageTarget, preferences, null, loginService, apiFactory);
    selector = CompositeUtil.findControl(shell, AccountSelector.class);
    projectID =
        CompositeUtil.findControlAfterLabel(shell, Text.class, "Cloud Platform &project ID:");
    stagingLocations =
        CompositeUtil.findControlAfterLabel(shell, Combo.class, "Cloud Storage staging &location:");
    createButton = CompositeUtil.findControl(shell, Button.class);
  }

  private void mockStorageApiBucketList(Credential credential, String... bucketNames)
      throws IOException {
    Storage storageApi = mock(Storage.class);
    Storage.Buckets bucketsApi = mock(Storage.Buckets.class);
    Storage.Buckets.List listApi = mock(Storage.Buckets.List.class);
    Buckets buckets = new Buckets();
    List<Bucket> bucketList = new ArrayList<>();

    when(apiFactory.newStorageApi(credential)).thenReturn(storageApi);
    when(storageApi.buckets()).thenReturn(bucketsApi);
    when(bucketsApi.list(anyString())).thenReturn(listApi);
    when(listApi.execute()).thenReturn(buckets);

    Storage.Buckets.Get exceptionGet = mock(Storage.Buckets.Get.class);
    when(bucketsApi.get(anyString())).thenReturn(exceptionGet);
    when(exceptionGet.execute()).thenThrow(new IOException("bucket does not exist"));

    for (String bucketName : bucketNames) {
      Bucket bucket = new Bucket();
      bucket.setName(bucketName);
      bucketList.add(bucket);

      Storage.Buckets.Get get = mock(Storage.Buckets.Get.class);
      when(bucketsApi.get(eq(bucketName))).thenReturn(get);
      when(get.execute()).thenReturn(bucket);
    }
    buckets.setItems(bucketList);
  }

  @Test
  public void testConstructor_testGrid() {
    try {
      new RunOptionsDefaultsComponent(null, 0, null, null);
      Assert.fail("didn't check grid");
    } catch (IllegalArgumentException ex) {
      Assert.assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void testCloudProjectText() {
    Assert.assertEquals("", component.getProject());
    component.setCloudProjectText("foo");
    Assert.assertEquals("foo", component.getProject());
  }

  @Test
  public void testGetControl() {
    Assert.assertSame(shell, component.getControl());
  }

  @Test
  public void testAccountSelector() {
    Assert.assertNotNull(selector);
  }

  @Test
  public void testAccountSelector_init() {
    Assert.assertEquals(2, selector.getAccountCount());

    int index1 = selector.selectAccount("alice@example.com");
    Assert.assertEquals(0, index1);
    Assert.assertEquals("alice@example.com", selector.getSelectedEmail());

    int index2 = selector.selectAccount("bob@example.com");
    Assert.assertEquals(1, index2);
    Assert.assertEquals("bob@example.com", selector.getSelectedEmail());
  }

  @Test
  public void testEnablement_initial() {
    assertTrue(selector.isEnabled());
    assertNull(selector.getSelectedCredential());
    assertFalse(projectID.isEnabled());
    assertFalse(stagingLocations.isEnabled());
    assertFalse(createButton.isEnabled());
  }

  @Test
  public void testEnablement_selectedAccount() {
    selector.selectAccount("alice@example.com");
    assertTrue(selector.isEnabled());
    assertNotNull(selector.getSelectedCredential());
    assertTrue(projectID.isEnabled());
    assertFalse(stagingLocations.isEnabled());
    assertFalse(createButton.isEnabled());
  }

  @Test
  public void testEnablement_selectedProject() {
    selector.selectAccount("alice@example.com");
    component.setCloudProjectText("project");
    assertTrue(selector.isEnabled());
    assertNotNull(selector.getSelectedCredential());
    assertTrue(projectID.isEnabled());
    assertTrue(stagingLocations.isEnabled());
    assertFalse(createButton.isEnabled());
  }

  @Test
  public void testEnablement_existingStagingLocation() throws InterruptedException {
    selector.selectAccount("alice@example.com");
    component.setCloudProjectText("project");
    component.setStagingLocationText("alice-bucket-1");
    component.startStagingLocationCheck(0); // force right now
    ListenableFuture<VerifyStagingLocationResult> verifyResult =
        component.verifyStagingLocationJob.getVerifyResult();
    int i = 0;
    do {
      while (Display.getCurrent().readAndDispatch()) {
        // spin
      }
      Thread.sleep(50);
    } while (i++ < 200 && !verifyResult.isDone());
    assertTrue(selector.isEnabled());
    assertNotNull(selector.getSelectedCredential());
    assertTrue(projectID.isEnabled());
    assertTrue(stagingLocations.isEnabled());
    assertFalse(createButton.isEnabled());
  }

  @Test
  public void testEnablement_nonExistentStagingLocation()
      throws OperationCanceledException, InterruptedException {
    selector.selectAccount("alice@example.com");
    component.setCloudProjectText("project");
    component.setStagingLocationText("non-existent-bucket");
    component.startStagingLocationCheck(0); // force right now
    ListenableFuture<VerifyStagingLocationResult> verifyResult =
        component.verifyStagingLocationJob.getVerifyResult();
    int i = 0;
    do {
      while (Display.getCurrent().readAndDispatch()) {
        // spin
      }
      Thread.sleep(50);
    } while(i++ < 200 && !createButton.isEnabled());
    assertTrue(verifyResult.isDone());
    assertTrue(selector.isEnabled());
    assertNotNull(selector.getSelectedCredential());
    assertTrue(projectID.isEnabled());
    assertTrue(stagingLocations.isEnabled());
    assertTrue(createButton.isEnabled());
  }

  @Test
  public void testStagingLocation() {
    selector.selectAccount("alice@example.com");
    component.setCloudProjectText("project");
    component.setStagingLocationText("foobar");
    Assert.assertEquals("gs://foobar", component.getStagingLocation());
  }

  @Test
  public void testAccountSelector_loadBucketCombo() throws InterruptedException {
    selector.selectAccount("alice@example.com");
    component.setCloudProjectText("some-gcp-project-id");
    component.updateStagingLocations("some-gcp-project-id", 0);

    assertStagingLocationCombo("gs://alice-bucket-1", "gs://alice-bucket-2");

    selector.selectAccount("bob@example.com");
    assertStagingLocationCombo("gs://bob-bucket");
  }

  private void assertStagingLocationCombo(String... buckets) throws InterruptedException {
    int i = 0;
    do {
      while (Display.getCurrent().readAndDispatch()) {
        // spin
      }
      Thread.sleep(50);
    } while(i++ < 200 && stagingLocations.getItemCount() != buckets.length);
    Assert.assertArrayEquals(buckets, stagingLocations.getItems());
  }

  @Test
  public void testBucketNameStatus_gcsPathWithObjectIsOk() {
    component.selectAccount("alice@example.com");
    component.setStagingLocationText("bucket/object");
    verify(messageTarget, never()).setError(anyString());
  }

  @Test
  public void testBucketNameStatus_gcsUrlPathWithObjectIsOk() {
    component.setStagingLocationText("gs://bucket/object");
    verify(messageTarget, never()).setError(anyString());
  }
}
