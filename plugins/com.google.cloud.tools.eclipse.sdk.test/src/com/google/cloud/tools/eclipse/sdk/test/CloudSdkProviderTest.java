
package com.google.cloud.tools.eclipse.sdk.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.sdk.CloudSdkProvider;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceConstants;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceInitializer;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;

public class CloudSdkProviderTest {

  /** Verify that the preference overrides PathResolver */
  @Test
  public void testSetPreferenceInvalid() throws Exception {
    // A path that almost certainly does not contain the SDK
    File root = File.listRoots()[0];
    PreferenceInitializer.getPreferenceStore().putValue(PreferenceConstants.CLOUDSDK_PATH,
        root.toString());
    CloudSdk.Builder builder = CloudSdkProvider.createBuilder(null);
    assertEquals(root.toPath(), getField(builder, "sdkPath", Path.class));
    CloudSdk instance = builder.build();
    assertEquals(root.toPath(), invoke(instance, "getSdkPath", Path.class));
    try {
      instance.validate();
      fail("root directory should not be a valid location");
    } catch (AppEngineException e) {
      // ignore
    }
  }

  private <T> T invoke(Object object, String methodName, Class<T> returnType, Object... paramters)
      throws NoSuchMethodException, SecurityException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
    Class<?>[] parameterTypes = new Class<?>[paramters.length];
    for (int i = 0; i < paramters.length; i++) {
      parameterTypes[i] = paramters[i] == null ? Object.class : paramters[i].getClass();
    }
    Method m = object.getClass().getDeclaredMethod(methodName, parameterTypes);
    m.setAccessible(true);
    return returnType.cast(m.invoke(object, paramters));
  }


  private <T> T getField(Object object, String fieldName, Class<T> fieldType)
      throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
      SecurityException {
    Field f = object.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    return fieldType.cast(f.get(object));
  }


}
