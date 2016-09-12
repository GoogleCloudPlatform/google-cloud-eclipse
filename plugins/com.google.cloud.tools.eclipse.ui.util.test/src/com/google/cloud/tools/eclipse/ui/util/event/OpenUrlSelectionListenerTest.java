package com.google.cloud.tools.eclipse.ui.util.event;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.ErrorHandler;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.ProjectIdProvider;

@RunWith(MockitoJUnitRunner.class)
public class OpenUrlSelectionListenerTest {

  private static final String VALID_URI = "http://example.org";
  private static final String INVALID_URI = "this is not an uri";
  private static final String MALFORMED_URL = "abcd://example.org";
  private static final String PROJECT_ID = "fake-project-id";
  
  @Mock private IWorkbenchBrowserSupport browserSupport;
  @Mock private IWebBrowser browser;
  @Mock private ErrorHandler errorHandler;
  @Mock private Widget widget;
  @Mock private ProjectIdProvider projectIdProvider;
  
  @Captor private ArgumentCaptor<Exception> captor;

  @Before
  public void setUp() throws PartInitException {
    when(browserSupport.getExternalBrowser()).thenReturn(browser);
  }

  private SelectionEvent getEvent(String uri) {
    Event event = new Event();
    event.widget = widget;
    event.text = uri;
    return new SelectionEvent(event);
  }

  @Test
  public void testWidgetSelected_InvalidURI() {
    SelectionEvent selectionEvent = getEvent(INVALID_URI);

    new OpenUriSelectionListener(projectIdProvider, errorHandler, browserSupport).widgetSelected(selectionEvent);
    verify(errorHandler).handle(captor.capture());
    assertThat(captor.getValue(), instanceOf(URISyntaxException.class));
  }

  @Test
  public void testWidgetDefaultSelected_InvalidURI() {
    SelectionEvent selectionEvent = getEvent(INVALID_URI);

    new OpenUriSelectionListener(projectIdProvider, errorHandler, browserSupport).widgetDefaultSelected(selectionEvent);
    verify(errorHandler).handle(captor.capture());
    assertThat(captor.getValue(), instanceOf(URISyntaxException.class));
  }

  @Test
  public void testWidgetSelected_MalformedURL() {
    SelectionEvent selectionEvent = getEvent(MALFORMED_URL);

    new OpenUriSelectionListener(projectIdProvider, errorHandler, browserSupport).widgetSelected(selectionEvent);
    verify(errorHandler).handle(captor.capture());
    assertThat(captor.getValue(), instanceOf(MalformedURLException.class));
  }

  @Test
  public void testWidgetDefaultSelected_MalformedURL() {
    SelectionEvent selectionEvent = getEvent(MALFORMED_URL);

    new OpenUriSelectionListener(projectIdProvider, errorHandler, browserSupport).widgetDefaultSelected(selectionEvent);
    verify(errorHandler).handle(captor.capture());
    assertThat(captor.getValue(), instanceOf(MalformedURLException.class));
  }

  @Test
  public void testWidgetSelected_errorInvokingBrowser() throws PartInitException {
    SelectionEvent selectionEvent = getEvent(VALID_URI);
    doThrow(new PartInitException("fake exception")).when(browser).openURL(any(URL.class));

    new OpenUriSelectionListener(projectIdProvider, errorHandler, browserSupport).widgetSelected(selectionEvent);
    verify(errorHandler).handle(captor.capture());
    assertThat(captor.getValue(), instanceOf(PartInitException.class));
  }

  @Test
  public void testWidgetDefaultSelected_errorInvokingBrowser() throws PartInitException {
    SelectionEvent selectionEvent = getEvent(VALID_URI);
    doThrow(new PartInitException("fake exception")).when(browser).openURL(any(URL.class));

    new OpenUriSelectionListener(projectIdProvider, errorHandler, browserSupport).widgetDefaultSelected(selectionEvent);
    verify(errorHandler).handle(captor.capture());
    assertThat(captor.getValue(), instanceOf(PartInitException.class));
  }

  @Test
  public void testWidgetSelected_successful() throws PartInitException, MalformedURLException {
    SelectionEvent selectionEvent = getEvent(VALID_URI);
    when(projectIdProvider.getProjectId()).thenReturn(PROJECT_ID);
    new OpenUriSelectionListener(projectIdProvider, errorHandler, browserSupport).widgetSelected(selectionEvent);
    verify(errorHandler, never()).handle(any(Exception.class));
    verify(browser).openURL(new URL(VALID_URI + "?project=" + PROJECT_ID));
  }

  @Test
  public void testWidgetDefaultSelected_successful() throws PartInitException, MalformedURLException {
    SelectionEvent selectionEvent = getEvent(VALID_URI);
    when(projectIdProvider.getProjectId()).thenReturn(PROJECT_ID);
    new OpenUriSelectionListener(projectIdProvider, errorHandler, browserSupport).widgetDefaultSelected(selectionEvent);
    verify(errorHandler, never()).handle(any(Exception.class));
    verify(browser).openURL(new URL(VALID_URI + "?project=" + PROJECT_ID));
  }
}
