package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexFacet;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractDeployPropertyPageForProjectsTest<P extends DeployPreferencesPanel> {

  protected static final IProjectFacetVersion APP_ENGINE_STANDARD_FACET_1 =
      ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID).getVersion("1");
  // commented out until Flex facet is enabled
//  protected static final IProjectFacetVersion APP_ENGINE_FLEX_FACET_1 =
//      ProjectFacetsManager.getProjectFacet(AppEngineFlexFacet.ID).getVersion("1");

  @Rule
  public ShellTestResource shellTestResource = new ShellTestResource();
  @Mock
  private IGoogleLoginService loginService;
  @Mock
  private IGoogleApiFactory googleApiFactory;

  public AbstractDeployPropertyPageForProjectsTest() {
    super();
  }

  @Test
  public void testCorrectPanelIsShownForFacetedProject() throws CoreException {
    DeployPropertyPage page = new DeployPropertyPage();
    Shell parent = shellTestResource.getShell();
    page.setElement(getProject());
    page.setLoginService(loginService);
    page.setGoogleApiFactory(googleApiFactory);
    page.createControl(parent);
    page.setVisible(true);
    Composite preferencePageComposite = (Composite) parent.getChildren()[0];
    for (Control control : preferencePageComposite.getChildren()) {
      if (control instanceof Composite) {
        Composite maybeDeployPageComposite = (Composite) control;
        Layout layout = maybeDeployPageComposite.getLayout();
        if (layout instanceof StackLayout) {
          StackLayout stackLayout = (StackLayout) layout;
          assertThat(stackLayout.topControl, instanceOf(getPanelClass()));
          return;
        }
      }
    }
    fail("Did not find the deploy preferences panel");
  }

  abstract protected IProject getProject();
  
  abstract protected Class<P> getPanelClass();

}