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

import com.google.cloud.tools.eclipse.appengine.deploy.ui.flexible.FlexDeployPreferencesPanel;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.standard.StandardDeployPreferencesPanel;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexWarFacet;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.util.AdapterUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.databinding.preference.PreferencePageSupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Displays the App Engine deployment page for the selected project in the property page dialog.
 * The contents of the App Engine deployment page vary depending on if the selected project
 * has the App Engine Standard facet, the App Engine flex facet, or no App Engine facet.
 */
public class DeployPropertyPage extends PropertyPage {

  private static final Logger logger = Logger.getLogger(DeployPropertyPage.class.getName());


  private IFacetedProject facetedProject = null;
  private FlexDeployPreferencesPanel flexPreferencesPanel;
  private StandardDeployPreferencesPanel standardPreferencesPanel;
  private BlankDeployPreferencesPanel blankPreferencesPanel;
  private PreferencePageSupport databindingSupport;
  private SharedScrolledComposite container;
  private DeployPreferencesPanel activeControl;

  public DeployPropertyPage() {  // 0-arg required for injection
  }

  @Override
  protected Control createContents(Composite parent) {
    container = new SharedScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL) {};
    container.setExpandHorizontal(true);
    container.setExpandVertical(true);
    container.setLayout(new GridLayout());

    IProject project = AdapterUtil.adapt(getElement(), IProject.class);

    try {
      facetedProject = ProjectFacetsManager.create(project);
    } catch (CoreException ex) {
      logger.log(Level.WARNING, ex.getMessage());
      return container;
    }

    GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
    evaluateFacetConfiguration();
    return container;
  }

  @Override
  public void dispose() {
    if (databindingSupport != null) {
      databindingSupport.dispose();
    }
    super.dispose();
  }

  private void handleLayoutChange() {
    container.reflow(true);
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
    return activeControl;
  }

  /**
   * Displays the appropriate deploy preferences panel based on the project's facet configuration.
   */
  private void evaluateFacetConfiguration() {
    if (databindingSupport != null) {
      databindingSupport.dispose();
    }
    if (facetedProject != null && AppEngineStandardFacet.hasFacet(facetedProject)) {
      createStandardPanelIfNeeded();
      showPanel(standardPreferencesPanel);
    } else if (facetedProject != null
        && ProjectFacetsManager.isProjectFacetDefined(AppEngineFlexWarFacet.ID)
        && AppEngineFlexWarFacet.hasFacet(facetedProject)) {
      createFlexPanelIfNeeded();
      showPanel(flexPreferencesPanel);
    } else {
      createBlankPanelIfNeeded();
      showPanel(blankPreferencesPanel);
    }
  }

  private void showPanel(DeployPreferencesPanel deployPreferencesPanel) {
    activeControl = deployPreferencesPanel;
    databindingSupport =
        PreferencePageSupport.create(this, deployPreferencesPanel.getDataBindingContext());
    PlatformUI.getWorkbench().getHelpSystem().setHelp(container.getParent(),
                                                      deployPreferencesPanel.getHelpContextId());
    container.setContent(deployPreferencesPanel);
  }

  private void createBlankPanelIfNeeded() {
    if (blankPreferencesPanel == null) {
      blankPreferencesPanel = new BlankDeployPreferencesPanel(container);
    }
  }

  private void createStandardPanelIfNeeded() {
    if (standardPreferencesPanel == null) {
      standardPreferencesPanel = new StandardDeployPreferencesPanel(
          container, facetedProject.getProject(), this::handleLayoutChange,
          false /* requireValues */, new ProjectRepository());
    }
  }

  private void createFlexPanelIfNeeded() {
    if (flexPreferencesPanel == null) {
      flexPreferencesPanel = new FlexDeployPreferencesPanel(
          container, facetedProject.getProject(), this::handleLayoutChange,
          false /* requireValues */, new ProjectRepository());
    }
  }
}
