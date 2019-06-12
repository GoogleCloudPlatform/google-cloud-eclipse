<#if package != "">package ${package};

</#if>import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
<#if servletVersion != "2.5">import java.util.Collection;
</#if>import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

/**
 * This mock class is created to enable basic unit testing of the
 * {@link HelloAppEngine} class. Only methods used in the unit test
 * have a non-trivial implementation.
 * 
 * Feel free to change this class or replace it using other ways for testing
 * {@link HttpServlet}s, e.g. Spring MVC Test or Mockito to suit your needs.
 */
class MockHttpServletResponse implements HttpServletResponse {

  private String contentType;
  private String encoding;
  private StringWriter writerContent = new StringWriter();
  private PrintWriter writer = new PrintWriter(writerContent);

  @Override
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return writer;
  }

  public StringWriter getWriterContent() {
    return writerContent;
  }

  // anything below is the default generated implementation

  @Override
  public void flushBuffer() throws IOException {
  }

  @Override
  public int getBufferSize() {
    return 0;
  }

  @Override
  public String getCharacterEncoding() {
    return encoding;
  }

  @Override
  public Locale getLocale() {
    return null;
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    return null;
  }

  @Override
  public boolean isCommitted() {
    return false;
  }

  @Override
  public void reset() {
  }

  @Override
  public void resetBuffer() {
  }

  @Override
  public void setBufferSize(int size) {
  }

  @Override
  public void setCharacterEncoding(String encoding) {
    this.encoding = encoding;
  }

  @Override
  public void setContentLength(int length) {
  }

  @Override
  public void setLocale(Locale locale) {
  }

  @Override
  public void addCookie(Cookie cookie) {
  }

  @Override
  public void addDateHeader(String name, long date) {
  }

  @Override
  public void addHeader(String name, String value) {
  }

  @Override
  public void addIntHeader(String name, int value) {
  }

  @Override
  public boolean containsHeader(String name) {
    return false;
  }

  @Override
  public String encodeRedirectURL(String url) {
    return null;
  }

  @Deprecated
  @Override
  public String encodeRedirectUrl(String url) {
    return null;
  }

  @Override
  public String encodeURL(String url) {
    return null;
  }

  @Deprecated
  @Override
  public String encodeUrl(String url) {
    return null;
  }

  @Override
  public void sendError(int statusCode) throws IOException {
  }

  @Override
  public void sendError(int statusCode, String message) throws IOException {
  }

  @Override
  public void sendRedirect(String url) throws IOException {
  }

  @Override
  public void setDateHeader(String name, long date) {
  }

  @Override
  public void setHeader(String name, String value) {
  }

  @Override
  public void setIntHeader(String name, int value) {
  }

  @Override
  public void setStatus(int statusCode) {
  }

  @Deprecated
  @Override
  public void setStatus(int statusCode, String message) {
  }

<#if servletVersion != "2.5">  // Servlet API 3.0 and 3.1 methods
  @Override
  public void setContentLengthLong(long length) {
  }

  @Override
  public int getStatus() {
    return 0;
  }

  @Override
  public String getHeader(String name) {
    return null;
  }

  @Override
  public Collection<String> getHeaders(String name) {
    return null;
  }

  @Override
  public Collection<String> getHeaderNames() {
    return null;
  }
</#if>}
