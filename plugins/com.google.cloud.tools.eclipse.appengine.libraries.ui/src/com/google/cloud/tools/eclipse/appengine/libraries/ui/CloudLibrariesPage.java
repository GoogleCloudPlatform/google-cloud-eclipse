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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension2;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.google.cloud.tools.eclipse.appengine.libraries.BuildPath;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineImages;
import com.google.cloud.tools.eclipse.appengine.ui.LibrarySelectorGroup;
import com.google.cloud.tools.eclipse.util.MavenUtils;

public abstract class CloudLibrariesPage extends WizardPage implements IClasspathContainerPage,
    IClasspathContainerPageExtension, IClasspathContainerPageExtension2 {

  private static final Logger logger = Logger.getLogger(CloudLibrariesPage.class.getName());
  private LibrarySelectorGroup librariesSelector;
  private IJavaProject project;
  private final String group;

  protected CloudLibrariesPage(String group) {
    super(group + "-page"); //$NON-NLS-1$
    setTitle(Messages.getString(group + "-title"));  //$NON-NLS-1$
    setDescription(Messages.getString(group + "-description"));  //$NON-NLS-1$
    setImageDescriptor(AppEngineImages.appEngine(64));
    this.group = group;
  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.BORDER);
    composite.setLayout(new GridLayout(2, true));
    
    librariesSelector = new LibrarySelectorGroup(composite, group);
    
    setControl(composite);
  }

  @Override
  public boolean finish() {
    return true;
  }

  @Override
  public IClasspathEntry getSelection() {
    // Since this class implements IClasspathContainerPageExtension2,
    // Eclipse calls getNewContainers instead.
    logger.log(Level.WARNING, "Unexpected call to getSelection()");
    return null;
  }

  @Override
  public void setSelection(IClasspathEntry containerEntry) {
  }

  @Override
  public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
    this.project = project;
  }

  @Override
  public IClasspathEntry[] getNewContainers() {
    List<Library> libraries = new ArrayList<>(librariesSelector.getSelectedLibraries());
    if (libraries == null || libraries.isEmpty()) {
      return null;
    }
  
    try {
      if (MavenUtils.hasMavenNature(project.getProject())) {
        BuildPath.addMavenLibraries(project.getProject(), libraries, new NullProgressMonitor());
        return new IClasspathEntry[0];
      } else {
        IClasspathEntry[] added =
            BuildPath.listAdditionalLibraries(project, libraries, new NullProgressMonitor());
        return added;
      }
    } catch (CoreException ex) {
      logger.log(Level.WARNING, "Error adding libraries to project", ex);
      return new IClasspathEntry[0];
    }
  }

}
