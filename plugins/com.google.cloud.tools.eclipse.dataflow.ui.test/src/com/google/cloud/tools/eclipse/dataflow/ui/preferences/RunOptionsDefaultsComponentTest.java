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

import static org.eclipse.swtbot.swt.finder.waits.Conditions.widgetIsEnabled;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.api.services.servicemanagement.ServiceManagement;
import com.google.api.services.servicemanagement.ServiceManagement.Services;
import com.google.api.services.servicemanagement.model.ListServicesResponse;
import com.google.api.services.servicemanagement.model.ManagedService;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.Buckets;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.project.VerifyStagingLocationJob.VerifyStagingLocationResult;
import com.google.cloud.tools.eclipse.dataflow.ui.page.MessageTarget;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.cloud.tools.login.Account;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.Future;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
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
  @Mock
  private WizardPage page;

  private SWTBot bot;
  private RunOptionsDefaultsComponent component;
  private Shell shell;

  private AccountSelector selector;
  private Combo projectID;
  private Combo stagingLocations;
  private Button createButton;


  @Before
  public void setUp() throws IOException {
    Account account1 = mock(Account.class, "alice@example.com");
    Credential credential1 = mock(Credential.class, "alice@example.com");
    when(account1.getEmail()).thenReturn("alice@example.com");
    when(account1.getOAuth2Credential()).thenReturn(credential1);
    mockStorageApiBucketList(credential1, "project", "alice-bucket-1", "alice-bucket-2");
    mockProjectList(credential1, new GcpProject("project", "project"));
    mockServiceApi(credential1, "project", "dataflow.googleapis.com");

    Account account2 = mock(Account.class, "bob@example.com");
    Credential credential2 = mock(Credential.class, "bob@example.com");
    when(account2.getEmail()).thenReturn("bob@example.com");
    when(account2.getOAuth2Credential()).thenReturn(credential2);
    mockStorageApiBucketList(credential2, "project", "bob-bucket");
    mockProjectList(credential2, new GcpProject("project", "project"));
    mockServiceApi(credential2, "project", "dataflow.googleapis.com");

    doCallRealMethod().when(page).setPageComplete(anyBoolean());
    doCallRealMethod().when(page).isPageComplete();

    when(loginService.getAccounts()).thenReturn(Sets.newHashSet(account1, account2));

    shell = shellResource.getShell();
    bot = new SWTBot(shell);
    component = new RunOptionsDefaultsComponent(
        shell, 3, messageTarget, preferences, page, false /* allowIncomplete */, loginService,
        apiFactory);
    selector = CompositeUtil.findControl(shell, AccountSelector.class);
    projectID =
        CompositeUtil.findControlAfterLabel(shell, Combo.class, "Cloud Platform &project ID:");
    stagingLocations =
        CompositeUtil.findControlAfterLabel(shell, Combo.class, "Cloud Storage staging &location:");
    createButton = CompositeUtil.findControl(shell, Button.class);
  }
  
  private void mockProjectList(Credential credential, GcpProject... gcpProjects) {
    Projects projectsApi = mock(Projects.class);
    Projects.List listApi = mock(Projects.List.class);
    List<Project> projectsList = new ArrayList<>();
    for (GcpProject gcpProject : gcpProjects) {
      Project project = new Project(); // cannot mock final classes
      project.setName(gcpProject.getName());
      project.setProjectId(gcpProject.getId());
      projectsList.add(project);
    }
    ListProjectsResponse response = new ListProjectsResponse(); // cannot mock final classes
    response.setProjects(projectsList);
    try {
      doReturn(projectsApi).when(apiFactory).newProjectsApi(credential);
      doReturn(listApi).when(listApi).setPageSize(any(Integer.class));
      doReturn(listApi).when(projectsApi).list();
      doReturn(response).when(listApi).execute();
    } catch (IOException ex) {
      fail(ex.toString());
    }
  }

  private void mockStorageApiBucketList(Credential credential, String projectId,
      String... bucketNames) throws IOException {
    Storage storageApi = mock(Storage.class);
    Storage.Buckets bucketsApi = mock(Storage.Buckets.class);
    Storage.Buckets.List listApi = mock(Storage.Buckets.List.class);
    Buckets buckets = new Buckets();
    List<Bucket> bucketList = new ArrayList<>();

    doReturn(storageApi).when(apiFactory).newStorageApi(credential);
    doReturn(bucketsApi).when(storageApi).buckets();
    doThrow(new IOException("not found")).when(bucketsApi).list(anyString());
    doReturn(listApi).when(bucketsApi).list(eq(projectId));
    doReturn(buckets).when(listApi).execute();

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

  private void mockServiceApi(Credential credential, String projectId, String... serviceIds)
      throws IOException {
    ServiceManagement servicesManagementApi = mock(ServiceManagement.class);
    Services servicesApi = mock(Services.class);
    Services.List request = mock(Services.List.class);
    ListServicesResponse response = new ListServicesResponse();
    
    doReturn(servicesManagementApi).when(apiFactory).newServiceManagementApi(credential);
    doReturn(servicesApi).when(servicesManagementApi).services();

    doReturn(request).when(servicesApi).list();
    // List provides a fluent API
    when(request.setFields(anyString())).thenReturn(request);
    when(request.setConsumerId(anyString())).thenReturn(request);
    when(request.setPageSize(anyInt())).thenReturn(request);
    when(request.setPageToken(anyString())).thenReturn(request);
    when(request.execute()).thenReturn(response);
    
    List<ManagedService> managedServices = new ArrayList<>();
    for (String serviceId : serviceIds) {
      ManagedService managedService = new ManagedService();
      managedService.setServiceName(serviceId);
      managedService.setProducerProjectId(projectId);
      managedServices.add(managedService);
    }
    response.setServices(managedServices);
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
    Assert.assertNull(component.getProject());
    selector.selectAccount("alice@example.com");
    component.setCloudProjectText("project");
    waitUntilResolvedProject();
    Assert.assertNotNull(component.getProject());
    Assert.assertEquals("project", component.getProject().getId());
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
    waitUntilResolvedProject();
    assertTrue(selector.isEnabled());
    assertNotNull(selector.getSelectedCredential());
    assertTrue(projectID.isEnabled());
    assertTrue(stagingLocations.isEnabled());
    assertFalse(createButton.isEnabled());
  }

  @Test
  public void testEnablement_nonExistentProject() throws InterruptedException {
    selector.selectAccount("alice@example.com");
    component.setCloudProjectText("doesnotexist");
    spinEvents();
    component.validate();
    assertTrue(selector.isEnabled());
    assertNotNull(selector.getSelectedCredential());
    assertTrue(projectID.isEnabled());
    assertFalse(stagingLocations.isEnabled());
    assertFalse(page.isPageComplete());
  }

  @Test
  public void testEnablement_existingStagingLocation() throws InterruptedException {
    selector.selectAccount("alice@example.com");
    component.setCloudProjectText("project");
    waitUntilResolvedProject();
    component.setStagingLocationText("alice-bucket-1");
    component.startStagingLocationCheck(0); // force right now
    final ListenableFuture<SortedSet<String>> fetchResult =
        component.fetchStagingLocationsJob.getFuture();
    final ListenableFuture<VerifyStagingLocationResult> verifyResult =
        component.verifyStagingLocationJob.getFuture();
    waitForFuture(verifyResult);
    waitForFuture(fetchResult);
    component.validate();
    assertTrue(selector.isEnabled());
    assertNotNull(selector.getSelectedCredential());
    assertTrue(projectID.isEnabled());
    assertTrue(stagingLocations.isEnabled());
    assertFalse(createButton.isEnabled());
    assertTrue(page.isPageComplete());
  }

  @Test
  public void testEnablement_nonExistentStagingLocation()
      throws OperationCanceledException, InterruptedException {
    selector.selectAccount("alice@example.com");
    component.setCloudProjectText("project");
    waitUntilResolvedProject();
    component.setStagingLocationText("non-existent-bucket");
    component.startStagingLocationCheck(0); // force right now
    ListenableFuture<VerifyStagingLocationResult> verifyResult =
        component.verifyStagingLocationJob.getFuture();
    waitForFuture(verifyResult);
    component.validate();
    bot.waitUntil(widgetIsEnabled(new SWTBotButton(createButton)));
    assertTrue(verifyResult.isDone());
    assertTrue(selector.isEnabled());
    assertNotNull(selector.getSelectedCredential());
    assertTrue(projectID.isEnabled());
    assertTrue(stagingLocations.isEnabled());
    assertTrue(createButton.isEnabled());
    assertFalse(page.isPageComplete());
  }

  @Test
  public void testStagingLocation() {
    selector.selectAccount("alice@example.com");
    component.setCloudProjectText("project");
    waitUntilResolvedProject();

    component.setStagingLocationText("foobar");
    Assert.assertEquals("gs://foobar", component.getStagingLocation());
  }

  @Test
  public void testAccountSelector_loadBucketCombo() throws InterruptedException {
    selector.selectAccount("alice@example.com");
    component.setCloudProjectText("project");
    waitUntilResolvedProject();
    waitForFuture(component.fetchStagingLocationsJob.getFuture());
    assertStagingLocationCombo("gs://alice-bucket-1", "gs://alice-bucket-2");

    selector.selectAccount("bob@example.com");
    waitUntilResolvedProject();
    waitForFuture(component.fetchStagingLocationsJob.getFuture());
    assertStagingLocationCombo("gs://bob-bucket");
  }

  private void assertStagingLocationCombo(final String... buckets) throws InterruptedException {
    bot.waitUntil(new DefaultCondition() {
      
      @Override
      public boolean test() throws Exception {
        return new SWTBotCombo(stagingLocations).itemCount() == buckets.length;
      }
      
      @Override
      public String getFailureMessage() {
        return "missing staging buckets";
      }
    });
    Assert.assertArrayEquals(buckets, stagingLocations.getItems());
  }

  @Test
  public void testBucketNameStatus_gcsPathWithObjectIsOk() {
    component.selectAccount("alice@example.com");
    component.setCloudProjectText("project");
    waitUntilResolvedProject();
    component.setStagingLocationText("alice-bucket-2/object");
    spinEvents();
    verify(messageTarget, never()).setError(anyString());
  }

  @Test
  public void testBucketNameStatus_gcsUrlPathWithObjectIsOk() {
    component.selectAccount("alice@example.com");
    component.setCloudProjectText("project");
    waitUntilResolvedProject();
    component.setStagingLocationText("gs://alice-bucket-2/object");
    spinEvents();
    verify(messageTarget, never()).setError(anyString());
  }

  @Test
  public void testPartialValidity_allEmpty() {
    component = new RunOptionsDefaultsComponent(shell, 3, messageTarget, preferences, page,
        true /* allowIncomplete */, loginService, apiFactory);
    assertTrue("should be complete when totally empty", page.isPageComplete());
  }
  
  @Test
  public void testPartialValidity_account() {
    testPartialValidity_allEmpty();      
    component.selectAccount("alice@example.com");
    assertTrue("should be complete with account", page.isPageComplete());
  }
  
  @Test
  public void testPartialValidity_account_project() throws InterruptedException {
    testPartialValidity_account();
    component.setCloudProjectText("project");
    int i = 0;
    do {
      while (Display.getCurrent().readAndDispatch()) {
        // spin
      }
      Thread.sleep(50);
    } while (i++ < 200 && !page.isPageComplete());

    assertTrue("should be complete with account and project", page.isPageComplete());
  }

  /**
   * Spin the display loop while the waitCondition is true or we timeout.
   * 
   * @param waitCondition
   */
  private void waitForFuture(final Future<?> future) {
    bot.waitUntil(new DefaultCondition() {
      @Override
      public boolean test() throws Exception {
        if (Display.getCurrent() != null) {
          // seems surprising that this is required?
          while (Display.getCurrent().readAndDispatch());
        }
        return future.isDone();
      }

      @Override
      public String getFailureMessage() {
        return "Future never done";
      }
    });
  }

  /**
   * Spin until the RunOptionsDefaultsComponent has a project.
   */
  private void waitUntilResolvedProject() {
    bot.waitUntil(new DefaultCondition() {
      @Override
      public boolean test() throws Exception {
        if (Display.getCurrent() != null) {
          // seems surprising that this is required?
          while (Display.getCurrent().readAndDispatch());
        }
        return component.getProject() != null;
      }

      @Override
      public String getFailureMessage() {
        return "RuntimeOptions project was never resolved";
      }
    });
  }

  /**
   * Spin the event loop once.
   */
  private void spinEvents() {
    // does a syncExec
    bot.shells();
  }


}
