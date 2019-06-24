/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.ui.status;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.ui.status.Incident.Severity;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.eclipse.ui.menus.UIElement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ShowGcpStatusHandlerTest {
  @Mock private UIElement element; 
  @Mock private Map<?,?> parameters; 
  @Mock private GcpStatusMonitoringService service;
  
  private ShowGcpStatusHandler fixture = new ShowGcpStatusHandler();
  
  @Test
  public void testUpdate_error() {
    @SuppressWarnings("unchecked")
    Collection<Incident> incidents = mock(Collection.class);
    when(service.getCurrentStatus())
        .thenReturn(new GcpStatus(Severity.ERROR, "summary", incidents));

    fixture.updateElement(element, parameters, service);
    
    verify(element).setIcon(ShowGcpStatusHandler.IMG_ERROR);
    verify(element).setText("Status: summary");
    verify(element).setTooltip("summary");
    verifyNoMoreInteractions(element, parameters, incidents); // incidents not used in ERROR
  }

  @Test
  public void testUpdate_high() {
    Incident incident = new MockIncident(Severity.HIGH, "incident");
    when(service.getCurrentStatus())
        .thenReturn(new GcpStatus(Severity.HIGH, "summary", Collections.singleton(incident)));

    fixture.updateElement(element, parameters, service);

    verify(element).setIcon(ShowGcpStatusHandler.IMG_HIGH);
    verify(element).setText("Status: summary");
    verify(element).setTooltip("incident");
    verifyNoMoreInteractions(element, parameters);
  }

  @Test
  public void testUpdate_medium() {
    Incident incident = new MockIncident(Severity.MEDIUM, "incident");
    when(service.getCurrentStatus())
        .thenReturn(new GcpStatus(Severity.MEDIUM, "summary", Collections.singleton(incident)));

    fixture.updateElement(element, parameters, service);

    verify(element).setIcon(ShowGcpStatusHandler.IMG_MEDIUM);
    verify(element).setText("Status: summary");
    verify(element).setTooltip("incident");
    verifyNoMoreInteractions(element, parameters);
  }

  @Test
  public void testUpdate_low() {
    Incident incident = new MockIncident(Severity.LOW, "incident");
    when(service.getCurrentStatus())
        .thenReturn(new GcpStatus(Severity.LOW, "summary", Collections.singleton(incident)));

    fixture.updateElement(element, parameters, service);

    verify(element).setIcon(ShowGcpStatusHandler.IMG_LOW);
    verify(element).setText("Status: summary");
    verify(element).setTooltip("incident");
    verifyNoMoreInteractions(element, parameters);
  }

  private static class MockIncident extends Incident {
    private final String toString;

    public MockIncident(Severity severity, String toString) {
      this.severity = severity;
      this.toString = toString;
    }

    @Override
    public String toString() {
      return toString;
    }
  }
}
