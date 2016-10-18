package com.google.cloud.tools.eclipse.appengine.libraries.persistence;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.JavaCore;

public class SerializableAccessRules {

  private AccessRuleKind ruleKind;
  private String pattern;

  public SerializableAccessRules(int kind, IPath pattern) {
    this.ruleKind = AccessRuleKind.forInt(kind);
    this.pattern = pattern.toString();
  }
  
  public IAccessRule toAccessRule() {
    return JavaCore.newAccessRule(new Path(pattern), ruleKind.kind);
  }
  
  private static enum AccessRuleKind {
    ACCESSIBLE(IAccessRule.K_ACCESSIBLE), 
    DISCOURAGED(IAccessRule.K_DISCOURAGED), 
    FORBIDDEN(IAccessRule.K_NON_ACCESSIBLE);
    
    int kind;

    AccessRuleKind(int kind) {
      this.kind = kind;
    }
    
    public static AccessRuleKind forInt(int kind) {
      switch (kind) {
      case IAccessRule.K_ACCESSIBLE:
        return ACCESSIBLE;
      case IAccessRule.K_DISCOURAGED:
        return DISCOURAGED;
      case IAccessRule.K_NON_ACCESSIBLE:
        return FORBIDDEN;
      default:
        throw new IllegalArgumentException("Invalid access rule kind value: " + kind);
      }
    }
  }
}