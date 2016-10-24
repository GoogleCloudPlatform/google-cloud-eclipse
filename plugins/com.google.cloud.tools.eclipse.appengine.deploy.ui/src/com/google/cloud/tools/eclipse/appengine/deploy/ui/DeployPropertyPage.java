/*******************************************************************************
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
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.databinding.preference.PreferencePageSupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexFacet;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.util.AdapterUtil;

/**
 * Displays the App Engine deployment page for the selected project in the property page dialog.
 * The contents of the App Engine deployment page vary depending on if the selected project
 * has the App Engine Standard facet, the App Engine flex facet, or no App Engine facet.
 */
public class DeployPropertyPage extends PropertyPage {

  private DeployPreferencesPanel content;
  private static final Logger logger = Logger.getLogger(DeployPropertyPage.class.getName());

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    try {
      content = getPreferencesPanel(container);
      if (content == null) {
        return container;
      }
    } catch (CoreException ex) {
      logger.log(Level.WARNING, ex.getMessage());
      return container;
    }

    GridDataFactory.fillDefaults().grab(true, false).applyTo(content);
    GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
    GridLayoutFactory.fillDefaults().generateLayout(container);

    PreferencePageSupport.create(this, content.getDataBindingContext());
    return content;
  }

  private Runnable getLayoutChangedHandler() {
    return new Runnable() {

      @Override
      public void run() {
        // resize the page to work around https://bugs.eclipse.org/bugs/show_bug.cgi?id=265237
        Composite parent = content.getParent();
        while (parent != null) {
          if (parent instanceof ScrolledComposite) {
            ScrolledComposite scrolledComposite = (ScrolledComposite) parent;
            scrolledComposite.setMinSize(content.getParent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
            content.layout();
            return;
          }
          parent = parent.getParent();
        }
      }
    };
  }

  @Override
  public boolean performOk() {
    if (isValid()) {
      return content.savePreferences();
    }
    return false;
  }

  @Override
  protected void performDefaults() {
    content.resetToDefaults();
    super.performDefaults();
  }

  @Override
  public void dispose() {
    content.dispose();
    super.dispose();
  }

  private DeployPreferencesPanel getPreferencesPanel(Composite container) throws CoreException {
    IProject project = AdapterUtil.adapt(getElement(), IProject.class);
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    IGoogleLoginService loginService =
        PlatformUI.getWorkbench().getService(IGoogleLoginService.class);

    if (AppEngineStandardFacet.hasAppEngineFacet(facetedProject)) {
      setTitle(Messages.getString("standard.page.title"));
      return new StandardDeployPreferencesPanel(
          container, project, loginService, getLayoutChangedHandler(), false /* requireValues */);
    } else if (AppEngineFlexFacet.hasAppEngineFacet(facetedProject)) {
      setTitle(Messages.getString("flex.page.title"));
      return new FlexDeployPreferencesPanel(container, project);
    } else {
      logger.log(Level.WARNING, "App Engine Deployment property page is only visible if project contains an App Engine facet");
      return null;
    }
  }
}
