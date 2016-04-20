package com.google.cloud.tools.eclipse.appengine.localserver.runtime;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.internal.Runtime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudSdkRuntimeTest {
  
  @Mock private Runtime runtime;
  @Mock private IPath mockLocation;
  
  @Test(expected = NullPointerException.class)
  public void testValidate_withoutRuntime() {
    new CloudSdkRuntime().validate();
  }
  
  @Test
  public void testValidate_withRuntimeWithoutName() throws Exception {
    CloudSdkRuntime cloudSdkRuntime = new CloudSdkRuntime();
    setField(cloudSdkRuntime, "runtime", runtime);
    cloudSdkRuntime.validate();
  }

  @Test
  public void testValidate_withRuntimeWithEmptyName() throws Exception {
    CloudSdkRuntime cloudSdkRuntime = new CloudSdkRuntime();
    setField(cloudSdkRuntime, "runtime", runtime);
    when(runtime.getName()).thenReturn("");
    IStatus status = cloudSdkRuntime.validate();
    assertThat(status.getSeverity(), is(IStatus.ERROR));
    assertThat(status.getMessage(), is("Enter a name for the runtime environment."));
  }

  @Test
  public void testValidate_withRuntimeWithNonEmptyNameNullPath() throws Exception {
    CloudSdkRuntime cloudSdkRuntime = new CloudSdkRuntime();
    setField(cloudSdkRuntime, "runtime", runtime);
    when(runtime.getName()).thenReturn("cloudSdkRuntime");
    IStatus status = cloudSdkRuntime.validate();
    assertThat(status.getSeverity(), is(IStatus.ERROR));
    assertThat(status.getMessage(), is(""));
  }

  @Test
  public void testValidate_withRuntimeWithNonEmptyNameEmptyLocation() throws Exception {
    CloudSdkRuntime cloudSdkRuntime = new CloudSdkRuntime();
    setField(cloudSdkRuntime, "runtime", runtime);
    when(runtime.getName()).thenReturn("cloudSdkRuntime");
    when(mockLocation.isEmpty()).thenReturn(true);
    when(runtime.getLocation()).thenReturn(mockLocation);
    IStatus status = cloudSdkRuntime.validate();
    assertThat(status.getSeverity(), is(IStatus.ERROR));
    assertThat(status.getMessage(), is(""));
  }

  @Test
  public void testValidate_withRuntimeWithNonEmptyNameNonExistentLocation() throws Exception {
    CloudSdkRuntime cloudSdkRuntime = new CloudSdkRuntime();
    setField(cloudSdkRuntime, "runtime", runtime);
    when(runtime.getName()).thenReturn("cloudSdkRuntime");
    when(mockLocation.isEmpty()).thenReturn(false);
    when(mockLocation.toString()).thenReturn("/non/existent/path");
    when(runtime.getLocation()).thenReturn(mockLocation);
    IStatus status = cloudSdkRuntime.validate();
    assertThat(status.getSeverity(), is(IStatus.ERROR));
    assertThat(status.getMessage(), is("Specified Cloud SDK directory does not exist"));
  }

  private <T, V> void setField(T obj, String fieldName, V value) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    Field field = getField(obj.getClass(), fieldName);
    if (field == null) {
      throw new NoSuchFieldException(fieldName);
    } else {
      field.setAccessible(true);
      field.set(obj, value);
    }
  }
  
  private Field getField(Class<?> clazz, String fieldName) {
    try {
      return clazz.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      clazz = clazz.getSuperclass();
      if (clazz == null) {
        return null;
      } else {
        return getField(clazz, fieldName);
      }
    }
  }
}
