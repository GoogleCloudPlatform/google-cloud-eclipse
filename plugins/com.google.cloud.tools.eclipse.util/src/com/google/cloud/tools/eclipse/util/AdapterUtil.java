package com.google.cloud.tools.eclipse.util;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;

public class AdapterUtil {

  private AdapterUtil() {}
  
  public static <T> T adapt(Object o, Class<T> cls) {
    if (cls.isInstance(o)) {
      return cls.cast(o);
    }
    if (o instanceof IAdaptable) {
      Object instance = ((IAdaptable) o).getAdapter(cls);
      if (instance != null) {
        return cls.cast(instance);
      }
    }
    Object adapted = Platform.getAdapterManager().loadAdapter(o, cls.getName());
    if (adapted != null) {
      return cls.cast(adapted);
    }
    return null;
  }
}
