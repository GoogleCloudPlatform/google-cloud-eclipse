package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.mockito.Mockito.mock;

import java.util.Hashtable;

import org.junit.rules.ExternalResource;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;

public class TestLibraryRepositoryService extends ExternalResource {

  private ServiceRegistration<ILibraryRepositoryService> serviceRegistration;
  private ILibraryRepositoryService repositoryService = mock(ILibraryRepositoryService.class);

  @Override
  protected void before() throws Throwable {
    registerMockService();
  }

  @Override
  protected void after() {
    if (serviceRegistration != null) {
      serviceRegistration.unregister();
    }
  }

  private void registerMockService() {
    Hashtable<String, Object> properties = new Hashtable<String, Object>();
    properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
    serviceRegistration = FrameworkUtil.getBundle(getClass()).getBundleContext()
        .registerService(ILibraryRepositoryService.class, getRepositoryService(), properties);
  }

  public ILibraryRepositoryService getRepositoryService() {
    return repositoryService;
  }
}
