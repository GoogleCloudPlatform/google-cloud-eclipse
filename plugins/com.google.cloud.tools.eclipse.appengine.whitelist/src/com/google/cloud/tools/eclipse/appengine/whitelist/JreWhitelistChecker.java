package com.google.cloud.tools.eclipse.appengine.whitelist;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

// see JavaCompilationParticipant.java in old plugin
public class JreWhitelistChecker extends CompilationParticipant {

  @Override
  public boolean isActive(IJavaProject project) {
    return false;
  }
  
}
