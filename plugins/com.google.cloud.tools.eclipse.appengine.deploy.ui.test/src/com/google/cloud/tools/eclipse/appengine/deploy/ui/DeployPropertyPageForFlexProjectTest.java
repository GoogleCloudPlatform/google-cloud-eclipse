package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.Arrays;
import org.eclipse.core.resources.IProject;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.Ignore;
import org.junit.Rule;

// until Flex facet is enabled
@Ignore
public class DeployPropertyPageForFlexProjectTest
    extends AbstractDeployPropertyPageForProjectsTest<FlexDeployPreferencesPanel> {

  // commented out until Flex facet is enabled
//  @Rule
//  public TestProjectCreator flexProjectCreator =
//      new TestProjectCreator().withFacetVersions(Arrays.asList(
//          JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25, APP_ENGINE_FLEX_FACET_1));

  @Override
  protected IProject getProject() {
    // commented out until Flex facet is enabled
//    return flexProjectCreator.getProject();
    return null;
  }

  @Override
  protected Class<FlexDeployPreferencesPanel> getPanelClass() {
    return FlexDeployPreferencesPanel.class;
  }
}
