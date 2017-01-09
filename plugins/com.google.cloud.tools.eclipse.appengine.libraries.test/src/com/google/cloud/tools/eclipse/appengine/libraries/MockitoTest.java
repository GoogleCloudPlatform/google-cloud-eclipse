package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MockitoTest {

  @Mock
  private ArrayList<Integer> list;
  
  @Test
  public void testMocking(){
    when(list.size()).thenReturn(-1);
    assertEquals(-1, list.size());
  }
}
