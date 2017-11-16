<#if package != "">package ${package};

</#if>import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public class HelloAppEngineMain {

  public static void main(String[] args) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
    server.createContext("/", new HelloAppEngineHandler());
    server.start();
  }
}