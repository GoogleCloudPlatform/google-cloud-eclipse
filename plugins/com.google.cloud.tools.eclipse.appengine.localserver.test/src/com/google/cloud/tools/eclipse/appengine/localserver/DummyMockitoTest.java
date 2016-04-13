package com.google.cloud.tools.eclipse.appengine.localserver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DummyMockitoTest {

  @Mock
  private List<Integer> list;

  @Test
  public void testMockito() {
    assertThat(list, notNullValue());
  }
}
