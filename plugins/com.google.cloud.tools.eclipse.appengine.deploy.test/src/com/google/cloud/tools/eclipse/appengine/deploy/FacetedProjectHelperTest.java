package com.google.cloud.tools.eclipse.appengine.deploy;

import static org.mockito.Mockito.mock;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.junit.Test;

public class FacetedProjectHelperTest {

  @Test(expected = NullPointerException.class)
  public void testGetFacetedProject_nullArgument() throws CoreException {
    new FacetedProjectHelper().getFacetedProject(null);
  }

  @Test(expected = NullPointerException.class)
  public void testProjectHasFacet_projectNull() {
    new FacetedProjectHelper().projectHasFacet(null, null);
  }

  @Test(expected = NullPointerException.class)
  public void testProjectHasFacet_facetIdNull() {
    new FacetedProjectHelper().projectHasFacet(mock(IFacetedProject.class), null);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testProjectHasFacet_facetIdEmpty() {
    new FacetedProjectHelper().projectHasFacet(mock(IFacetedProject.class), "");
  }
}
