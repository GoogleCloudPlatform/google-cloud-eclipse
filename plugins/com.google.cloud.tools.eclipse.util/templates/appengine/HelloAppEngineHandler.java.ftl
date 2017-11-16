<#if package != "">package ${package};

</#if>import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HelloAppEngineHandler implements HttpHandler {

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    String response = "Hello App Engine!";
    httpExchange.sendResponseHeaders(200, response.length());
    try (OutputStream out = httpExchange.getResponseBody()) {
      out.write(response.getBytes(StandardCharsets.UTF_8));
    }
  }
}