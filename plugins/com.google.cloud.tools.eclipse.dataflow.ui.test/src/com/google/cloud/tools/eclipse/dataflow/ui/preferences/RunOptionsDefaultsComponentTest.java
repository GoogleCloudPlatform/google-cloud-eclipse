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
import static org.junit.Assert.assertEquals;
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

import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.api.services.servicemanagement.ServiceManagement;
import com.google.api.services.servicemanagement.ServiceManagement.Services;
import com.google.api.services.servicemanagement.model.ListServicesResponse;
import com.google.api.services.servicemanagement.model.ManagedService;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.Storage.Buckets.Insert;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.Buckets;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.ui.page.MessageTarget;
import com.google.cloud.tools.eclipse.dataflow.ui.preferences.RunOptionsDefaultsComponent.ValidationStatus;
import com.google.cloud.tools.eclipse.googleapis.Account;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RunOptionsDefaultsComponentTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  @Mock private DataflowPreferences preferences;
  @Mock private MessageTarget messageTarget;
  @Mock private GoogleApiFactory apiFactory;
  @Mock private WizardPage page;

  private SWTBot bot;
  private RunOptionsDefaultsComponent component;
  private Shell shell;

  private AccountSelector selector;
  private Combo projectID;
  private Combo stagingLocations;
  private Button createButton;
  private Text serviceAccountKey;
  private Button browse;

  @Before
  public void setUp() throws IOException {
    logout(false /* forceAccountCheck*/);
    
    doCallRealMethod().when(page).setPageComplete(anyBoolean());
    doCallRealMethod().when(page).isPageComplete();
    GoogleApiFactory.setInstance(apiFactory);
    shell = shellResource.getShell();
    bot = new SWTBot(shell);
    component = new RunOptionsDefaultsComponent(
        shell, 3, messageTarget, preferences, page, false /* allowIncomplete */);
    selector = CompositeUtil.findControl(shell, AccountSelector.class);
    projectID =
        CompositeUtil.findControlAfterLabel(shell, Combo.class, "Cloud Platform &project ID:");
    stagingLocations =
        CompositeUtil.findControlAfterLabel(shell, Combo.class, "Cloud Storage staging &location:");
    createButton = CompositeUtil.findButton(shell, "&Create Bucket");
    serviceAccountKey = CompositeUtil.findControl(shell, Text.class);
    browse = CompositeUtil.findButton(shell, "Browse...");
  }
  
  @After
  public void tearDown() {
    GoogleApiFactory.resetInstance();
  }
  
  private boolean loginAlice() throws IOException {
    return setUpApiFactory(Optional.of(TestAccountProvider.ACCOUNT_1), true);
  }
  private boolean loginBob() throws IOException {
    return setUpApiFactory(Optional.of(TestAccountProvider.ACCOUNT_2), true);
  }
  private boolean logout() throws IOException {
    return setUpApiFactory(Optional.empty(), true);
  }
  private boolean logout(boolean forceAccountCheck) throws IOException {
    return setUpApiFactory(Optional.empty(), forceAccountCheck);
  }
  
  /**
   * 
   * @param account
   * @param forceAccountCheck
   * @return true if an account change was detected in {@code AccountSelector}
   */
  private boolean setUpApiFactory(Optional<Account> account, boolean forceAccountCheck) 
    throws IOException {
    Preconditions.checkNotNull(account);
    if (!account.isPresent()) {
      when(apiFactory.getAccount()).thenReturn(Optional.empty());
      when(apiFactory.getCredential()).thenReturn(Optional.empty());
    } else {
      when(apiFactory.getAccount()).thenReturn(account);
      when(apiFactory.getCredential()).thenReturn(Optional.of(account.get().getOAuth2Credential()));
      if (account.get().equals(TestAccountProvider.ACCOUNT_1)) {
        mockStorageApiBucketList("project", "alice-bucket-1", "alice-bucket-2");
        mockProjectList(new GcpProject("project", "project"));
        mockServiceApi("project", "dataflow.googleapis.com");
      } else {
        mockStorageApiBucketList("project", "bob-bucket");
        mockProjectList(new GcpProject("project", "project"));
        mockServiceApi("project", "dataflow.googleapis.com");
      }
    }
    if (forceAccountCheck) {
      return selector.forceAccountCheck();
    }
   
    return false;
  }

  private void mockProjectList(GcpProject... gcpProjects)
      throws IOException {
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
    doReturn(projectsApi).when(apiFactory).newProjectsApi();
    doReturn(listApi).when(listApi).setPageSize(anyInt());
    doReturn(listApi).when(projectsApi).list();
    doReturn(response).when(listApi).execute();
  }

  private void mockStorageApiBucketList(String projectId,
      String... bucketNames) throws IOException {
    Storage storageApi = mock(Storage.class);
    Storage.Buckets bucketsApi = mock(Storage.Buckets.class);
    Storage.Buckets.List listApi = mock(Storage.Buckets.List.class);
    Buckets buckets = new Buckets();
    List<Bucket> bucketList = new ArrayList<>();

    doReturn(storageApi).when(apiFactory).newStorageApi();
    doReturn(bucketsApi).when(storageApi).buckets();
    doThrow(new IOException("not found")).when(bucketsApi).list(anyString());
    doReturn(listApi).when(bucketsApi).list(projectId);
    doReturn(buckets).when(listApi).execute();
    
    when(bucketsApi.insert(eq(projectId), any(Bucket.class))).thenAnswer(invocationOnMock -> {
      Bucket newBucket = invocationOnMock.getArgumentAt(1, Bucket.class);
      Insert insert = mock(Insert.class);
      when(insert.execute()).thenAnswer(unused -> {
        bucketList.add(newBucket);
        return newBucket;
      });
      return insert;
    });

    Storage.Buckets.Get exceptionGet = mock(Storage.Buckets.Get.class);
    when(bucketsApi.get(anyString())).thenReturn(exceptionGet);
    when(exceptionGet.execute()).thenThrow(new IOException("bucket does not exist"));

    for (String bucketName : bucketNames) {
      Bucket bucket = new Bucket();
      bucket.setName(bucketName);
      bucketList.add(bucket);

      Storage.Buckets.Get get = mock(Storage.Buckets.Get.class);
      when(bucketsApi.get(bucketName)).thenReturn(get);
      when(get.execute()).thenReturn(bucket);
    }
    buckets.setItems(bucketList);
  }

  private void mockServiceApi(String projectId, String... serviceIds)
      throws IOException {
    ServiceManagement servicesManagementApi = mock(ServiceManagement.class);
    Services servicesApi = mock(Services.class);
    Services.List request = mock(Services.List.class);
    ListServicesResponse response = new ListServicesResponse();

    doReturn(servicesManagementApi).when(apiFactory).newServiceManagementApi();
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
  public void testCloudProjectText() throws IOException {
    Assert.assertNull(component.getProject());
    loginAlice();
    component.setCloudProjectText("project");
    join();
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
  public void testAccountSelector_init() throws IOException {
    logout();
    Assert.assertEquals(0, selector.getAccountCount());

    loginAlice();
    Assert.assertEquals(TestAccountProvider.EMAIL_ACCOUNT_1, selector.getSelectedEmail());

    loginBob();
    Assert.assertEquals(TestAccountProvider.EMAIL_ACCOUNT_2, selector.getSelectedEmail());
  }

  @Test
  public void testEnablement_initial() {
    assertTrue(selector.isEnabled());
    assertNull(selector.getSelectedCredential());
    assertFalse(projectID.isEnabled());
    assertFalse(stagingLocations.isEnabled());
    assertFalse(createButton.isEnabled());
    assertTrue(serviceAccountKey.isEnabled());
    assertTrue(browse.isEnabled());
  }

  @Test
  public void testEnablement_selectedAccount() throws IOException {
    loginAlice();
    assertTrue(selector.isEnabled());
    assertNotNull(selector.getSelectedCredential());
    assertTrue(projectID.isEnabled());
    assertFalse(stagingLocations.isEnabled());
    assertFalse(createButton.isEnabled());
    assertTrue(serviceAccountKey.isEnabled());
    assertTrue(browse.isEnabled());
  }

  @Test
  public void testEnablement_selectedProject() throws IOException {
    loginAlice();
    component.setCloudProjectText("project");
    join();
    assertTrue(selector.isEnabled());
    assertNotNull(selector.getSelectedCredential());
    assertTrue(projectID.isEnabled());
    assertTrue(stagingLocations.isEnabled());
    assertFalse(createButton.isEnabled());
    assertTrue(serviceAccountKey.isEnabled());
    assertTrue(browse.isEnabled());
  }

  @Test
  public void testEnablement_nonExistentProject() throws IOException {
    loginAlice();
    component.setCloudProjectText("doesnotexist");
    spinEvents();
    ValidationStatus result = component.validate();
    assertEquals(ValidationStatus.NULL_PROJECT, result);
    assertTrue(selector.isEnabled());
    assertNotNull(selector.getSelectedCredential());
    assertTrue(projectID.isEnabled());
    assertFalse(stagingLocations.isEnabled());
    assertFalse(page.isPageComplete());
    assertTrue(serviceAccountKey.isEnabled());
    assertTrue(browse.isEnabled());
  }

  @Test
  public void testEnablement_existingStagingLocation() throws IOException {
    loginAlice();
    component.setCloudProjectText("project");
    join();
    component.setStagingLocationText("alice-bucket-1");
    component.startStagingLocationCheck(0); // force right now
    join();
    ValidationStatus result = component.validate();
    assertEquals(ValidationStatus.BUCKET_ACCESSIBLE, result);
    assertTrue(selector.isEnabled());
    assertNotNull(selector.getSelectedCredential());
    assertTrue(projectID.isEnabled());
    assertTrue(stagingLocations.isEnabled());
    assertFalse(createButton.isEnabled());
    assertTrue(page.isPageComplete());
    assertTrue(serviceAccountKey.isEnabled());
    assertTrue(browse.isEnabled());
  }

  @Test
  public void testEnablement_nonExistentStagingLocation() throws IOException {
    loginAlice();
    component.setCloudProjectText("project");
    join();
    component.setStagingLocationText("non-existent-bucket");
    join();
    ValidationStatus result = component.validate();
    spinEvents();
    join();
    join();
    assertTrue(component.canEnableChildren());
    assertTrue(selector.isEnabled());
    assertNotNull(selector.getSelectedCredential());
    assertFalse(component.getControl().isDisposed());
    assertNotNull(component.getProject());
    verifyProjectsLoaded("project");
    assertEquals(ValidationStatus.BUCKET_CAN_BE_CREATED, result);
    bot.waitUntil(widgetIsEnabled(new SWTBotButton(createButton)));
    assertTrue(projectID.isEnabled());
    assertTrue(stagingLocations.isEnabled());
    assertTrue(createButton.isEnabled());
    assertFalse(page.isPageComplete());
    assertTrue(serviceAccountKey.isEnabled());
    assertTrue(browse.isEnabled());
  }
  
  private void verifyProjectsLoaded(String... projectIds) throws IOException {
    List<Project> projects = apiFactory.newProjectsApi().list().execute().getProjects();
    for (String projectId : projectIds) {
      assertTrue(projects.stream().anyMatch(project -> project.getProjectId() == projectId));
    }
  }

  @Test
  public void testGetServiceAccountKey() {
    serviceAccountKey.setText("/some/random/file.ext");
    assertEquals("/some/random/file.ext", component.getServiceAccountKey());
  }

  @Test
  public void testEnablement_disabledWhileAllValuesValid() throws IOException {
    loginAlice();
    component.setCloudProjectText("project");
    join();
    component.setStagingLocationText("alice-bucket-1");
    component.startStagingLocationCheck(0); // force right now
    join();
    component.validate();
    assertTrue(projectID.isEnabled());
    assertTrue(stagingLocations.isEnabled());
    assertTrue(page.isPageComplete());

    component.setEnabled(false);
    component.validate();
    assertFalse(projectID.isEnabled());
    assertFalse(stagingLocations.isEnabled());
    assertTrue(page.isPageComplete());
  }

  @Test
  public void testStagingLocation() throws IOException {
    loginAlice();
    component.setCloudProjectText("project");
    join();

    component.setStagingLocationText("foobar");
    Assert.assertEquals("gs://foobar", component.getStagingLocation());
  }

  @Test
  public void testAccountSelector_loadBucketCombo() throws IOException {
    loginAlice();
    component.setCloudProjectText("project");
    join();
    assertStagingLocationCombo("gs://alice-bucket-1", "gs://alice-bucket-2");

    loginBob();
    join();
    assertStagingLocationCombo("gs://bob-bucket");
  }

  private void assertStagingLocationCombo(final String... buckets) {
    bot.waitUntil(new DefaultCondition() {

      @Override
      public boolean test() throws Exception {
        spinEvents();
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
  public void testBucketNameStatus_gcsPathWithObjectIsOk() throws IOException {
    loginAlice();
    component.setCloudProjectText("project");
    join();
    component.setStagingLocationText("alice-bucket-2/object");
    join();
    verify(messageTarget, never()).setError(anyString());
  }

  @Test
  public void testBucketNameStatus_gcsUrlPathWithObjectIsOk() throws IOException {
    loginAlice();
    component.setCloudProjectText("project");
    join();
    component.setStagingLocationText("gs://alice-bucket-2/object");
    join();
    verify(messageTarget, never()).setError(anyString());
  }
  
  @Test
  public void testBucketNameStatus_createIsOk() throws IOException {
    loginAlice();
    component.setCloudProjectText("project");
    join();
    component.setStagingLocationText("gs://alice-bucket-non-existent");
    join();
    Mockito.reset(messageTarget);
    component.validate();
    verify(messageTarget).setError("Could not fetch bucket alice-bucket-non-existent.");
    
    Mockito.reset(messageTarget);
    new SWTBotButton(createButton).click();
    join();
    verify(messageTarget).setInfo("Created staging location at gs://alice-bucket-non-existent");
  }

  @Test
  public void testValidity_nonExistingServiceAccountKey() {
    serviceAccountKey.setText("/non/existing/file.ext");
    verify(messageTarget).setError("/non/existing/file.ext does not exist.");
  }

  @Test
  public void testValidity_directoryAsServiceAccountKey() {
    serviceAccountKey.setText("/");
    verify(messageTarget).setError(Matchers.contains(" is a directory."));
  }

  @Test
  public void testPartialValidity_allEmpty() {
    component = new RunOptionsDefaultsComponent(shell, 3, messageTarget, preferences, page,
        true /* allowIncomplete */);
    assertTrue("should be complete when totally empty", page.isPageComplete());
  }

  @Test
  public void testPartialValidity_invalidServiceAccountKey() {
    component = new RunOptionsDefaultsComponent(shell, 3, messageTarget, preferences, page,
        true /* allowIncomplete */);
    serviceAccountKey.setText("/non/existing/file.ext");

    assertFalse("should be incomplete with invalid service key even if allowInComplete is true",
        page.isPageComplete());
  }

  @Test
  public void testPartialValidity_account() throws IOException {
    testPartialValidity_allEmpty();
    assertTrue(loginAlice());
    join();
    assertFalse(Strings.isNullOrEmpty(component.getAccountEmail()));
    assertTrue(Strings.isNullOrEmpty(component.getServiceAccountKey()));
    assertTrue(component.doIsolatedQuickChecks());
    assertEquals(ValidationStatus.NULL_PROJECT, component.validate());
    assertTrue("should be complete with account", page.isPageComplete());
  }

  @Test
  public void testPartialValidity_account_project() throws IOException {
    testPartialValidity_account();
    component.setCloudProjectText("project");
    join();
    assertTrue("should be complete with account and project", page.isPageComplete());
  }

  @Test
  public void testAlignButtons() {
    Button width13 = mock(Button.class);
    Button width18 = mock(Button.class);
    Button width10 = mock(Button.class);
    when(width13.computeSize(anyInt(), anyInt())).thenReturn(new Point(13, 0));
    when(width18.computeSize(anyInt(), anyInt())).thenReturn(new Point(18, 0));
    when(width10.computeSize(anyInt(), anyInt())).thenReturn(new Point(10, 0));

    RunOptionsDefaultsComponent.alignButtons(width13, width18, width10);

    ArgumentCaptor<GridData> captor = ArgumentCaptor.forClass(GridData.class);  // reusable
    verify(width13).setLayoutData(captor.capture());
    assertEquals(18, captor.getValue().widthHint);

    verify(width18).setLayoutData(captor.capture());
    assertEquals(18, captor.getValue().widthHint);

    verify(width10).setLayoutData(captor.capture());
    assertEquals(18, captor.getValue().widthHint);
  }

  /**
   * Spin until the RunOptionsDefaultsComponent has a project.
   */
  private void join() {
    try {
      while (Display.getCurrent().readAndDispatch());
      component.join();
      while (Display.getCurrent().readAndDispatch());
      component.join();
      while (Display.getCurrent().readAndDispatch());
    } catch (InterruptedException ex) {
      fail(ex.toString());
    }
  }

  /**
   * Spin the event loop once.
   */
  private void spinEvents() {
    // does a syncExec
    bot.shells();
  }

}
