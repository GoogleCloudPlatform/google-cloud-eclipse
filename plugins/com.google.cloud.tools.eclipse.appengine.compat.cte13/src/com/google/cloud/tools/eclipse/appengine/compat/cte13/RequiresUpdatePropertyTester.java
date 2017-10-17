
package com.google.cloud.tools.eclipse.appengine.compat.cte13;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;

public class RequiresUpdatePropertyTester extends PropertyTester {
  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    return receiver instanceof IProject
        && CloudToolsEclipseProjectUpdater.hasOldContainers((IProject) receiver);
  }
}
