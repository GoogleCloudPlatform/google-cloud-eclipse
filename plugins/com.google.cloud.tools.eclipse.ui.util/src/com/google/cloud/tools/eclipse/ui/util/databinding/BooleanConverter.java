package com.google.cloud.tools.eclipse.ui.util.databinding;

import org.eclipse.core.databinding.conversion.Converter;

public abstract class BooleanConverter extends Converter {

  private static BooleanConverter NEGATE_INSTANCE = new BooleanConverter() {
    public Object convert(Object fromObject) {
      return !(Boolean)fromObject;
    }
  };

  public static BooleanConverter negate() {
    return NEGATE_INSTANCE;
  }

  protected BooleanConverter() {
    super(Boolean.class, Boolean.class);
  }
}