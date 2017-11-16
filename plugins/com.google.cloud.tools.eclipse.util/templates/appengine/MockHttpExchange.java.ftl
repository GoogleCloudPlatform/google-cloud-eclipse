<#if package != "">package ${package};

</#if>import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class MockHttpExchange extends HttpExchange {

  private int responseCode;
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

  public String getWrittenContents() throws UnsupportedEncodingException {
	return outputStream.toString("UTF-8");
  }

  @Override
  public void close() {
  }

  @Override
  public Object getAttribute(String name) {
    return null;
  }

  @Override
  public HttpContext getHttpContext() {
    return null;
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return null;
  }

  @Override
  public HttpPrincipal getPrincipal() {
    return null;
  }

  @Override
  public String getProtocol() {
    return null;
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return null;
  }

  @Override
  public InputStream getRequestBody() {
    return null;
  }

  @Override
  public Headers getRequestHeaders() {
    return null;
  }

  @Override
  public String getRequestMethod() {
    return null;
  }

  @Override
  public URI getRequestURI() {
    return null;
  }

  @Override
  public OutputStream getResponseBody() {
    return outputStream;
  }

  @Override
  public int getResponseCode() {
    return responseCode;
  }

  @Override
  public Headers getResponseHeaders() {
    return null;
  }

  @Override
  public void sendResponseHeaders(int responseCode, long responseLength) throws IOException {
    this.responseCode = responseCode;
  }

  @Override
  public void setAttribute(String name, Object value) {
  }

  @Override
  public void setStreams(InputStream in, OutputStream out) {
  }

}