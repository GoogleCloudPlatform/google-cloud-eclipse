package com.google.cloud.tools.eclipse.ui.util.databinding;

import org.eclipse.core.databinding.conversion.Converter;

public abstract class BooleanConverter extends Converter {

  public static BooleanConverter negate() {
     return new BooleanConverter() {
        public Object convert(Object fromObject) {
           return !(Boolean)fromObject;
        }
     };
  }
  
  protected BooleanConverter() {
     super(Boolean.class, Boolean.class);
  }
}