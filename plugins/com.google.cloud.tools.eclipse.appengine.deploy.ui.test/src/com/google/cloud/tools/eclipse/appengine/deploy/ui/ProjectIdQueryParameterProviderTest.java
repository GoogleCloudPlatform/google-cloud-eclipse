package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.swt.widgets.Text;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ProjectIdQueryParameterProviderTest {

  private Text projectId;
  private ProjectIdQueryParameterProvider instance;
  
  @Before
  public void setUp() {
    projectId = Mockito.mock(Text.class);
    instance = new ProjectIdQueryParameterProvider(projectId);
  }
  
  @Test
  public void testEmpty() {
    Mockito.when(projectId.getText()).thenReturn("   ");
    Assert.assertTrue(instance.getParameters().isEmpty());
  }
  
  @Test
  public void testNotEmpty() {
    Mockito.when(projectId.getText()).thenReturn("myId");
    Assert.assertEquals(1, instance.getParameters().size());
  }

}
