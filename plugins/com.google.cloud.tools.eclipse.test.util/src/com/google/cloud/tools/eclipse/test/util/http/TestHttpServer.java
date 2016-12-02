package com.google.cloud.tools.eclipse.test.util.http;

import com.google.common.base.Preconditions;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

/**
 * Simple HTTP server (wrapping an embedded Jetty server) to serve a file on a random available port.
 * <p>
 * Use {@link #getAddress()} to obtain the server's address after it has been started via the {@link #before()} method.
 */
public class TestHttpServer extends ExternalResource {

  private static final Logger logger = Logger.getLogger(TestHttpServer.class.getName());

  private TemporaryFolder temporaryFolder;
  private Server server;
  private String fileName;
  private String fileContent;

  public TestHttpServer(TemporaryFolder temporaryFolder, String fileName, String fileContent) {
    this.temporaryFolder = temporaryFolder;
    this.fileName = fileName;
    this.fileContent = fileContent;
  }

  @Override
  protected void before() throws Exception {
    runServer();
  }

  @Override
  protected void after() {
    stopServer();
  }

  private void runServer() throws Exception {
    server = new Server(new InetSocketAddress("127.0.0.1", 0));
    ResourceHandler resourceHandler = new ResourceHandler();

    File resourceBase = temporaryFolder.newFolder();
    java.nio.file.Path fileToServe = Files.createFile(resourceBase.toPath().resolve(fileName));
    Files.write(fileToServe, fileContent.getBytes(StandardCharsets.UTF_8));
    resourceHandler.setResourceBase(resourceBase.getAbsolutePath());

    HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[] { resourceHandler, new DefaultHandler() });
    server.setHandler(handlers);

    server.dumpStdErr();
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
   * Returns the address that can be used to get resources from the server.
   * <p>
   * Initialized only after the server has started.
   *
   * @return server address in the form of http://127.0.0.1:&lt;port&gt;
   */
  public String getAddress() {
    Preconditions.checkNotNull(server, "server isn't started yet");
    // assume a single server connector
    return "http://127.0.0.1:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort();
  }
}