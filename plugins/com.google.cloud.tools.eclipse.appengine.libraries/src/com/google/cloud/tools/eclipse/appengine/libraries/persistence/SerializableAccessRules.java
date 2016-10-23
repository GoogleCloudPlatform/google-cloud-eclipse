/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.libraries.persistence;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.JavaCore;

/**
 * Represents a {@link IAccessRule} in such a way that it can be easily transformed into JSON.
 */
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
