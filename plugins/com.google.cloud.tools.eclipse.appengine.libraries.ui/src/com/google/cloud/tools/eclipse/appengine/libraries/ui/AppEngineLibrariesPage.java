/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.libraries.ui;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.google.cloud.tools.eclipse.appengine.ui.AppEngineImages;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineLibrariesSelectorGroup;

public class AppEngineLibrariesPage extends WizardPage 
    implements IClasspathContainerPage, IClasspathContainerPageExtension {

  public AppEngineLibrariesPage() {
    super("App Engine Libraries Page"); //$NON-NLS-1$
    setTitle("App Engine Standard Environment Libraries");
    setDescription("Additional jars commonly used in App Engine Standard Environment applications");
    setImageDescriptor(AppEngineImages.appEngine(64));
  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.BORDER);
    composite.setLayout(new GridLayout(2, true));
    
    AppEngineLibrariesSelectorGroup group = new AppEngineLibrariesSelectorGroup(composite);
    
    setControl(composite);
  }

  @Override
  public boolean finish() {
    return true;
  }

  @Override
  public IClasspathEntry getSelection() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setSelection(IClasspathEntry containerEntry) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
    // TODO Auto-generated method stub
    
  }

}