package com.google.cloud.tools.eclipse.test.util.http;

import static org.junit.Assert.assertTrue;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.rules.ExternalResource;

/**
 * Simple HTTP server (wrapping an embedded Jetty server) that listens on a random available port.
 * <p>
 * Use {@link #getAddress()} to obtain the server's address after it has been started via the
 * {@link #before()} method.
 */
public class TestHttpServer extends ExternalResource {

  private static final Logger logger = Logger.getLogger(TestHttpServer.class.getName());

  private Server server;

  private boolean requestHandled = false;

  private String requestMethod;
  private Map<String, String[]> requestParameters;
  private final Map<String, String> requestHeaders = new HashMap<>();

  private final String expectedPath;
  private final String responseContent;

  // Examples: new TestHttpServer("folder/sample.txt", "arbitrary file content");
  //           new TestHttpServer("", "<html><body>root</body></html>");
  public TestHttpServer(String expectedPath, String responseContent) {
    this.expectedPath = expectedPath;
    this.responseContent = responseContent;
  }

  @Override
  protected void before() throws Exception {
    runServer();
  }

  @Override
  protected void after() {
    stopServer();
    assertTrue(requestHandled);
  }

  private void runServer() throws Exception {
    server = new Server(new InetSocketAddress("127.0.0.1", 0));
    server.setHandler(new RequestHandler());
    server.start();
  }

  private void stopServer() {
    try {
      server.stop();
      server.join();
    } catch (Exception ex) {
      // probably should not fail the test, but if it happens it should be visible in the logs
      logger.log(Level.WARNING, "Error while shutting down Jetty server", ex);
    }
  }

  /**
   * Returns the address that can be used to send requests to the server.
   * <p>
   * Initialized only after the server has started.
   *
   * @return server address in the form of http://127.0.0.1:&lt;port&gt;/
   */
  public String getAddress() {
    Preconditions.checkNotNull(server, "server isn't started yet");
    // assume a single server connector
    return "http://" + getHostname() + ":" + getPort() + "/";
  }

  public String getHostname() {
    return "127.0.0.1";
  }

  public int getPort() {
    return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
  }

  public String getRequestMethod() {
    Preconditions.checkState(requestHandled);
    return requestMethod;
  }

  public Map<String, String[]> getRequestParameters() {
    Preconditions.checkState(requestHandled);
    return requestParameters;
  }

  public Map<String, String> getRequestHeaders() {
    Preconditions.checkState(requestHandled);
    return requestHeaders;
  }

  private class RequestHandler extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
      Preconditions.checkState(!requestHandled);

      if (request.getContentType() != null
          && request.getContentType().startsWith("multipart/form-data")) {
        request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT,
            new MultipartConfigElement(System.getProperty("java.io.tmpdir")));
      }

      if (target.equals("/" + expectedPath)) {
        requestHandled = true;
        requestMethod = request.getMethod();
        requestParameters = request.getParameterMap();
        for (Enumeration<String> headers = request.getHeaderNames(); headers.hasMoreElements(); ) {
          String header = headers.nextElement();
          requestHeaders.put(header, request.getHeader(header));
        }

        baseRequest.setHandled(true);
        byte[] bytes = responseContent.getBytes(StandardCharsets.UTF_8);
        response.getOutputStream().write(bytes);
        response.setStatus(HttpServletResponse.SC_OK);
      }
    }
  }
}
