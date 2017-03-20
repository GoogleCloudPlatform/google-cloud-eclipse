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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexFacet;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.util.AdapterUtil;
import com.google.common.annotations.VisibleForTesting;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.databinding.preference.PreferencePageSupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Displays the App Engine deployment page for the selected project in the property page dialog.
 * The contents of the App Engine deployment page vary depending on if the selected project
 * has the App Engine Standard facet, the App Engine flex facet, or no App Engine facet.
 */
public class DeployPropertyPage extends PropertyPage {

  private static final Logger logger = Logger.getLogger(DeployPropertyPage.class.getName());

  @Inject
  private IGoogleLoginService loginService;
  @Inject
  private IGoogleApiFactory googleApiFactory;

  private IFacetedProject facetedProject = null;
  private FlexDeployPreferencesPanel flexPreferencesPanel;
  private StandardDeployPreferencesPanel standardPreferencesPanel;
  private BlankDeployPreferencesPanel blankPreferencesPanel;
  private StackLayout stackLayout;
  private PreferencePageSupport databindingSupport;
  private Composite container;

  @Override
  protected Control createContents(Composite parent) {
    container = new Composite(parent, SWT.NONE);
    stackLayout = new StackLayout();
    container.setLayout(stackLayout);

    IProject project = AdapterUtil.adapt(getElement(), IProject.class);

    try {
      facetedProject = ProjectFacetsManager.create(project);
    } catch (CoreException ex) {
      logger.log(Level.WARNING, ex.getMessage());
      return container;
    }

    GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

    getControl().addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        if (databindingSupport != null) {
          databindingSupport.dispose();
        }
      }
    });
    return container;
  }

  private Runnable getLayoutChangedHandler() {
    return new Runnable() {

      @Override
      public void run() {
        // resize the page to work around https://bugs.eclipse.org/bugs/show_bug.cgi?id=265237
        Composite parent = getActivePanel().getParent();
        while (parent != null) {
          if (parent instanceof ScrolledComposite) {
            ScrolledComposite scrolledComposite = (ScrolledComposite) parent;
            scrolledComposite.setMinSize(getActivePanel().getParent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
            getActivePanel().layout();
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
      return getActivePanel().savePreferences();
    }
    return false;
  }

  @Override
  protected void performDefaults() {
    // should we reset all panels?
    getActivePanel().resetToDefaults();
    super.performDefaults();
  }

  private DeployPreferencesPanel getActivePanel() {
    return (DeployPreferencesPanel) stackLayout.topControl;
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      evaluateFacetConfiguration();
    }
  }

  /**
   * Displays the appropriate deploy preferences panel based on the project's facet configuration.
   */
  private void evaluateFacetConfiguration() {
    if (databindingSupport != null) {
      databindingSupport.dispose();
    }
    if (facetedProject != null && AppEngineStandardFacet.hasAppEngineFacet(facetedProject)) {
      createStandardPanelIfNeeded();
      showPanel(standardPreferencesPanel);
    } else if (facetedProject != null 
        && ProjectFacetsManager.isProjectFacetDefined(AppEngineFlexFacet.ID) 
        && AppEngineFlexFacet.hasAppEngineFacet(facetedProject)) {
      createFlexPanelIfNeeded();
      showPanel(flexPreferencesPanel);
    } else {
      createBlankPanelIfNeeded();
      showPanel(blankPreferencesPanel);
    }
    container.layout();
  }

  private void showPanel(DeployPreferencesPanel deployPreferencesPanel) {
    stackLayout.topControl = deployPreferencesPanel;
    databindingSupport =
        PreferencePageSupport.create(this, deployPreferencesPanel.getDataBindingContext());
    PlatformUI.getWorkbench().getHelpSystem().setHelp(container.getParent(),
                                                      deployPreferencesPanel.getHelpContextId());
  }

  private void createBlankPanelIfNeeded() {
    if (blankPreferencesPanel == null) {
      blankPreferencesPanel = new BlankDeployPreferencesPanel(container);
    }
  }

  private void createStandardPanelIfNeeded() {
    if (standardPreferencesPanel == null) {
      standardPreferencesPanel = new StandardDeployPreferencesPanel(
          container, facetedProject.getProject(), loginService, getLayoutChangedHandler(),
          false /* requireValues */, new ProjectRepository(googleApiFactory));
    }
  }

  private void createFlexPanelIfNeeded() {
    if (flexPreferencesPanel == null) {
      flexPreferencesPanel = new FlexDeployPreferencesPanel(container, facetedProject.getProject());
    }
  }

  /**
   * Used only in tests.
   */
  @VisibleForTesting
  void setLoginService(IGoogleLoginService loginService) {
    this.loginService = loginService;
  }

  /**
   * Used only in tests.
   */
  @VisibleForTesting
  void setGoogleApiFactory(IGoogleApiFactory googleApiFactory) {
    this.googleApiFactory = googleApiFactory;
  }
}
